package com.finai.service;

import com.finai.dto.finance.RawTransaction;
import com.finai.exception.FinaiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estrae i movimenti grezzi da un estratto conto in formato Excel (.xlsx/.xls)
 * o PDF, con parsing euristico data + descrizione + importo.
 *
 * <p>Non esiste un formato standard di estratto conto: il parsing è basato su
 * pattern comuni (colonna "Importo" con segno, oppure coppia Dare/Avere per
 * Excel; righe testuali "data — descrizione — importo" per PDF). Movimenti
 * che non corrispondono a nessun pattern riconosciuto vengono scartati senza
 * interrompere l'import del resto del file.</p>
 */
@Service
public class StatementParserService {

    private static final Logger log = LoggerFactory.getLogger(StatementParserService.class);

    private static final Set<String> DATE_HEADERS   = Set.of("data", "data contabile", "data valuta", "date");
    private static final Set<String> AMOUNT_HEADERS = Set.of("importo", "amount", "valore");
    private static final Set<String> DEBIT_HEADERS  = Set.of("dare", "uscite", "addebito", "addebiti", "debit");
    private static final Set<String> CREDIT_HEADERS = Set.of("avere", "entrate", "accredito", "accrediti", "credit");
    private static final Set<String> DESC_HEADERS    = Set.of("descrizione", "causale", "operazione", "description", "dettaglio");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
    );

    /** Riga composta solo da una data, senza descrizione/importo: tipico di estratti conto con layout a tabella
     *  dove ogni cella viene estratta dal PDF su una riga separata. */
    private static final Pattern PDF_DATE_ONLY = Pattern.compile("^(\\d{1,2}[/.\\-]\\d{1,2}(?:[/.\\-]\\d{2,4})?)$");

    /** Riga composta solo da un importo, senza data/descrizione (cella di una tabella). */
    private static final Pattern PDF_AMOUNT_ONLY = Pattern.compile("^[+-]?(?:\\d{1,3}(?:[.,]\\d{3})*|\\d+)[.,]\\d{2}\\s*(?:EUR|€)?$");

    private static final Set<String> INCOME_KEYWORDS = Set.of(
            "stipendio", "accredito", "bonifico in entrata", "bonifico a vostro favore", "bonifico ricevuto",
            "versamento", "rimborso", "pensione", "salary", "incasso", "entrata", "entrate",
            "giroconto a favore", "interess", "dividend", "cedola", "storno"
    );

    private static final Set<String> OPENING_BALANCE_KEYWORDS = Set.of(
            "saldo iniziale", "saldo precedente", "saldo di apertura", "saldo al", "opening balance"
    );

    /**
     * Riconosce l'estensione del file e applica il parser corretto.
     *
     * @param filename nome originale del file (usato per scegliere xlsx/xls vs pdf)
     * @param input    contenuto del file
     */
    public List<RawTransaction> parse(String filename, InputStream input) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".pdf")) {
                return parsePdf(input);
            }
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return parseExcel(input);
            }
        } catch (Exception e) {
            log.warn("Errore parsing estratto conto {}: {}", filename, e.getMessage());
            throw new FinaiException("Impossibile leggere il file: " + e.getMessage(), 422);
        }
        throw new FinaiException("Formato file non supportato. Usa PDF, XLSX o XLS.", 422);
    }

    // ─────────────────────────────────── Excel ────────────────────────────────

    List<RawTransaction> parseExcel(InputStream input) throws IOException {
        List<RawTransaction> results = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(input)) {
            Sheet sheet = wb.getSheetAt(0);
            ColumnLayout layout = detectLayout(sheet);

            int startRow = sheet.getFirstRowNum() + (layout.headerRow ? 1 : 0);
            for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                try {
                    RawTransaction tx = layout.headerRow ? parseRowWithLayout(row, layout) : parseRowHeuristic(row);
                    if (tx != null) results.add(tx);
                } catch (Exception e) {
                    log.debug("Riga Excel ignorata ({}): {}", r, e.getMessage());
                }
            }
        }
        return results;
    }

    private record ColumnLayout(boolean headerRow, int dateCol, int descCol, int amountCol, int debitCol, int creditCol) {}

    private ColumnLayout detectLayout(Sheet sheet) {
        Row header = sheet.getRow(sheet.getFirstRowNum());
        if (header == null) return new ColumnLayout(false, -1, -1, -1, -1, -1);

        int dateCol = -1, descCol = -1, amountCol = -1, debitCol = -1, creditCol = -1;
        for (Cell cell : header) {
            String text = cellAsString(cell).toLowerCase(Locale.ROOT).trim();
            int idx = cell.getColumnIndex();
            if (DATE_HEADERS.contains(text) && dateCol < 0) dateCol = idx;
            else if (AMOUNT_HEADERS.contains(text) && amountCol < 0) amountCol = idx;
            else if (DEBIT_HEADERS.contains(text) && debitCol < 0) debitCol = idx;
            else if (CREDIT_HEADERS.contains(text) && creditCol < 0) creditCol = idx;
            else if (DESC_HEADERS.contains(text) && descCol < 0) descCol = idx;
        }
        boolean recognized = dateCol >= 0 && (amountCol >= 0 || debitCol >= 0 || creditCol >= 0);
        return new ColumnLayout(recognized, dateCol, descCol, amountCol, debitCol, creditCol);
    }

    private RawTransaction parseRowWithLayout(Row row, ColumnLayout layout) {
        LocalDate date = cellAsDate(row.getCell(layout.dateCol()));
        if (date == null) return null;

        BigDecimal amount;
        if (layout.amountCol() >= 0) {
            amount = cellAsAmount(row.getCell(layout.amountCol()));
        } else {
            BigDecimal debit = layout.debitCol() >= 0 ? cellAsAmount(row.getCell(layout.debitCol())) : null;
            BigDecimal credit = layout.creditCol() >= 0 ? cellAsAmount(row.getCell(layout.creditCol())) : null;
            BigDecimal d = debit != null ? debit.abs() : BigDecimal.ZERO;
            BigDecimal c = credit != null ? credit.abs() : BigDecimal.ZERO;
            amount = c.subtract(d);
        }
        if (amount == null) return null;

        String description = layout.descCol() >= 0
                ? cellAsString(row.getCell(layout.descCol()))
                : guessDescription(row, layout.dateCol());
        return new RawTransaction(date, description.isBlank() ? "Movimento" : description, amount);
    }

    /** Nessuna riga di intestazione riconosciuta: cerca data + importo per posizione. */
    private RawTransaction parseRowHeuristic(Row row) {
        LocalDate date = null;
        int dateIdx = -1;
        List<BigDecimal> numbers = new ArrayList<>();
        List<Integer> numberIdx = new ArrayList<>();
        String description = null;

        for (Cell cell : row) {
            if (date == null) {
                LocalDate d = cellAsDate(cell);
                if (d != null) {
                    date = d;
                    dateIdx = cell.getColumnIndex();
                    continue;
                }
            }
            if (cell.getCellType() == CellType.NUMERIC) {
                numbers.add(BigDecimal.valueOf(cell.getNumericCellValue()));
                numberIdx.add(cell.getColumnIndex());
            } else if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();
                if (!text.isEmpty() && (description == null || text.length() > description.length())) {
                    description = text;
                }
            }
        }
        if (date == null || numbers.isEmpty()) return null;

        BigDecimal amount;
        if (numbers.size() == 1) {
            amount = numbers.get(0);
        } else {
            BigDecimal first = numbers.get(0);
            BigDecimal second = numbers.get(1);
            if (first.signum() != 0 && second.signum() == 0) amount = first.abs().negate();
            else if (second.signum() != 0 && first.signum() == 0) amount = second.abs();
            else amount = second.subtract(first);
        }
        return new RawTransaction(date, description == null || description.isBlank() ? "Movimento" : description, amount);
    }

    private String guessDescription(Row row, int dateCol) {
        String best = null;
        for (Cell cell : row) {
            if (cell.getColumnIndex() == dateCol) continue;
            if (cell.getCellType() == CellType.STRING) {
                String text = cell.getStringCellValue().trim();
                if (!text.isEmpty() && (best == null || text.length() > best.length())) best = text;
            }
        }
        return best == null ? "" : best;
    }

    private String cellAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    private LocalDate cellAsDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            if (cell.getCellType() == CellType.STRING) {
                return parseDateText(cell.getStringCellValue().trim());
            }
        } catch (Exception ignored) { }
        return null;
    }

    private BigDecimal cellAsAmount(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                return parseAmountText(cell.getStringCellValue());
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ─────────────────────────────────── PDF ───────────────────────────────────

    List<RawTransaction> parsePdf(InputStream input) throws IOException {
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            String text = new PDFTextStripper().getText(doc);
            List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                    .map(String::trim).filter(l -> !l.isEmpty()).toList();

            List<RawTransaction> rows = parsePdfRows(lines);
            if (!rows.isEmpty()) return rows;

            // Fallback: estratti conto dove PDFBox estrae ogni cella (data / descrizione /
            // importo / saldo) su una riga separata invece che sulla stessa riga.
            return parsePdfTableLayout(lines);
        }
    }

    private static final Pattern DATE_PREFIX = Pattern.compile(
            "^(\\d{1,2}[/.\\-]\\d{1,2}(?:[/.\\-]\\d{2,4})?)\\s+(.*)$");
    private static final Pattern AMOUNT_TOKEN = Pattern.compile(
            "^[+-]?(?:\\d{1,3}(?:[.,]\\d{3})*|\\d+)[.,]\\d{2}$");

    private record PdfRow(LocalDate date, String description, List<String> amountTokens) {}

    /**
     * Estratti conto "a riga" (una riga di testo per movimento): copre sia il formato semplice
     * "data descrizione importo[+segno]" sia quello a colonne "data descrizione entrata/uscita
     * saldo", dove PDFBox estrae l'intera riga della tabella come un'unica stringa con uno o due
     * numeri finali.
     */
    private List<RawTransaction> parsePdfRows(List<String> lines) {
        List<PdfRow> rows = new ArrayList<>();
        for (String line : lines) {
            PdfRow row = parseRow(line);
            if (row != null) rows.add(row);
        }
        if (rows.isEmpty()) return List.of();

        boolean hasBalanceColumn = rows.stream().anyMatch(r -> r.amountTokens().size() >= 2);

        List<RawTransaction> results = new ArrayList<>();
        BigDecimal runningBalance = null;
        for (int idx = 0; idx < rows.size(); idx++) {
            PdfRow row = rows.get(idx);
            List<String> amounts = row.amountTokens();

            if (amounts.size() == 1) {
                String token = amounts.get(0);
                BigDecimal value = parseAmountText(token);
                if (value == null) continue;

                if (hasExplicitSign(token)) {
                    results.add(new RawTransaction(row.date(), row.description(), value));
                    continue;
                }
                boolean isOpeningBalance = hasBalanceColumn && (idx == 0 || isOpeningBalanceDescription(row.description()));
                if (isOpeningBalance) {
                    runningBalance = value;
                    continue;
                }
                // Nessun segno "-" davanti all'importo: per il formato di questo estratto conto
                // significa entrata (stipendio, regalo, ecc.), coerentemente con le righe a segno
                // esplicito già gestite sopra (dove "-" indica sempre un'uscita).
                results.add(new RawTransaction(row.date(), row.description(), value.abs()));
                continue;
            }

            // Due o più numeri sulla riga: l'ultimo è il saldo, il penultimo il movimento.
            BigDecimal movement = parseAmountText(amounts.get(amounts.size() - 2));
            BigDecimal newBalance = parseAmountText(amounts.get(amounts.size() - 1));
            if (movement == null || newBalance == null) continue;
            movement = movement.abs();

            BigDecimal signed = runningBalance != null
                    ? (newBalance.compareTo(runningBalance) >= 0 ? movement : movement.negate())
                    : (isIncomeDescription(row.description()) ? movement : movement.negate());
            runningBalance = newBalance;
            results.add(new RawTransaction(row.date(), row.description(), signed));
        }
        return results;
    }

    /** Estrae data, descrizione e gli importi finali (1 o 2) da un'intera riga di testo. */
    private PdfRow parseRow(String line) {
        Matcher dm = DATE_PREFIX.matcher(line);
        if (!dm.matches()) return null;
        LocalDate date = parseDateText(dm.group(1));
        if (date == null) return null;

        String[] tokens = dm.group(2).trim().split("\\s+");
        List<String> amountTokens = new ArrayList<>();
        int end = tokens.length;
        while (end > 0) {
            String tok = tokens[end - 1];
            if (tok.equalsIgnoreCase("EUR") || tok.equals("€")) { end--; continue; }
            if (AMOUNT_TOKEN.matcher(tok).matches()) { amountTokens.add(0, tok); end--; continue; }
            break;
        }
        if (amountTokens.isEmpty() || amountTokens.size() > 2) return null;

        String description = String.join(" ", Arrays.asList(tokens).subList(0, end)).trim();
        if (description.isEmpty()) description = "Movimento";
        return new PdfRow(date, description, amountTokens);
    }

    private record PdfBlock(LocalDate date, String description, List<BigDecimal> amounts) {}

    /**
     * Estratti conto "a tabella" (data / descrizione / entrate / uscite / saldo), dove ogni
     * cella diventa una riga separata nel testo estratto. Raggruppa le righe a partire da
     * ogni data trovata, raccoglie la descrizione e gli importi successivi, e deduce il segno
     * del movimento confrontando il saldo corrente con quello della riga precedente (quando
     * è presente una colonna saldo), altrimenti tramite parole chiave di entrata/uscita.
     */
    private List<RawTransaction> parsePdfTableLayout(List<String> lines) {
        List<PdfBlock> blocks = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            if (!PDF_DATE_ONLY.matcher(line).matches()) { i++; continue; }
            LocalDate date = parseDateText(line);
            i++;
            if (date == null) continue;

            String description = "Movimento";
            if (i < lines.size()
                    && !PDF_AMOUNT_ONLY.matcher(lines.get(i)).matches()
                    && !PDF_DATE_ONLY.matcher(lines.get(i)).matches()) {
                description = lines.get(i);
                i++;
            }

            List<BigDecimal> amounts = new ArrayList<>();
            while (i < lines.size() && PDF_AMOUNT_ONLY.matcher(lines.get(i)).matches()) {
                BigDecimal amount = parseAmountText(lines.get(i));
                if (amount != null) amounts.add(amount);
                i++;
            }
            if (!amounts.isEmpty()) blocks.add(new PdfBlock(date, description, amounts));
        }

        boolean hasBalanceColumn = blocks.stream().anyMatch(b -> b.amounts().size() >= 2);

        List<RawTransaction> results = new ArrayList<>();
        BigDecimal runningBalance = null;
        for (int idx = 0; idx < blocks.size(); idx++) {
            PdfBlock block = blocks.get(idx);

            if (block.amounts().size() == 1) {
                BigDecimal value = block.amounts().get(0);
                boolean isOpeningBalance = hasBalanceColumn
                        && (idx == 0 || isOpeningBalanceDescription(block.description()));
                if (isOpeningBalance) {
                    runningBalance = value;
                    continue;
                }
                // Nessun segno "-" davanti all'importo: entrata (vedi commento sopra in parsePdfRows).
                results.add(new RawTransaction(block.date(), block.description(), value.abs()));
                continue;
            }

            BigDecimal movement = block.amounts().get(block.amounts().size() - 2).abs();
            BigDecimal newBalance = block.amounts().get(block.amounts().size() - 1);
            BigDecimal signed = runningBalance != null
                    ? (newBalance.compareTo(runningBalance) >= 0 ? movement : movement.negate())
                    : (isIncomeDescription(block.description()) ? movement : movement.negate());
            runningBalance = newBalance;
            results.add(new RawTransaction(block.date(), block.description(), signed));
        }
        return results;
    }

    private boolean isIncomeDescription(String description) {
        String lower = description.toLowerCase(Locale.ROOT);
        return INCOME_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean isOpeningBalanceDescription(String description) {
        String lower = description.toLowerCase(Locale.ROOT);
        return OPENING_BALANCE_KEYWORDS.stream().anyMatch(lower::contains);
    }

    private boolean hasExplicitSign(String amountText) {
        return amountText.startsWith("+") || amountText.startsWith("-");
    }

    // ─────────────────────────────────── Helpers ───────────────────────────────

    private static final Pattern DATE_NO_YEAR = Pattern.compile("^(\\d{1,2})[/.\\-](\\d{1,2})$");

    private LocalDate parseDateText(String text) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException ignored) { }
        }
        // Data senza anno (es. "01/07"): assume l'anno corrente, oppure quello precedente
        // se la data risulterebbe nel futuro (tipico di estratti conto a cavallo di fine anno).
        Matcher noYear = DATE_NO_YEAR.matcher(text.trim());
        if (noYear.matches()) {
            try {
                int day = Integer.parseInt(noYear.group(1));
                int month = Integer.parseInt(noYear.group(2));
                LocalDate candidate = LocalDate.of(LocalDate.now().getYear(), month, day);
                if (candidate.isAfter(LocalDate.now())) candidate = candidate.minusYears(1);
                return candidate;
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** Converte un importo testuale italiano ("1.234,56" o "-45,00") in BigDecimal. */
    private BigDecimal parseAmountText(String text) {
        if (text == null) return null;
        String cleaned = text.trim().replace("€", "").replace("EUR", "").trim();
        boolean negative = cleaned.startsWith("-");
        cleaned = cleaned.replaceFirst("^[+-]", "");
        // Formato italiano: punto = migliaia, virgola = decimale
        cleaned = cleaned.replace(".", "").replace(",", ".");
        try {
            BigDecimal value = new BigDecimal(cleaned);
            return negative ? value.negate() : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
