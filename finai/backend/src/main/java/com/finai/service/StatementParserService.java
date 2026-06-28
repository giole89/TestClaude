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

    private static final Pattern PDF_LINE = Pattern.compile(
            "^\\s*(\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4})\\s+(.+?)\\s+([+-]?\\d{1,3}(?:[.,]\\d{3})*[.,]\\d{2})\\s*(?:EUR|€)?\\s*$"
    );

    private static final Set<String> INCOME_KEYWORDS = Set.of(
            "stipendio", "accredito", "bonifico in entrata", "bonifico a vostro favore",
            "versamento", "rimborso", "pensione", "salary", "incasso"
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
        List<RawTransaction> results = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(input.readAllBytes())) {
            String text = new PDFTextStripper().getText(doc);
            for (String line : text.split("\\r?\\n")) {
                Matcher m = PDF_LINE.matcher(line);
                if (!m.matches()) continue;

                LocalDate date = parseDateText(m.group(1));
                String description = m.group(2).trim();
                BigDecimal amount = parseAmountText(m.group(3));
                if (date == null || amount == null) continue;

                if (!hasExplicitSign(m.group(3)) && !description.startsWith("-")) {
                    boolean isIncome = INCOME_KEYWORDS.stream()
                            .anyMatch(k -> description.toLowerCase(Locale.ROOT).contains(k));
                    amount = isIncome ? amount.abs() : amount.abs().negate();
                }
                results.add(new RawTransaction(date, description, amount));
            }
        }
        return results;
    }

    private boolean hasExplicitSign(String amountText) {
        return amountText.startsWith("+") || amountText.startsWith("-");
    }

    // ─────────────────────────────────── Helpers ───────────────────────────────

    private LocalDate parseDateText(String text) {
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, fmt);
            } catch (DateTimeParseException ignored) { }
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
