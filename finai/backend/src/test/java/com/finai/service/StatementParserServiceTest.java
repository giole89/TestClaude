package com.finai.service;

import com.finai.dto.finance.RawTransaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitari per {@link StatementParserService}.
 */
@DisplayName("StatementParserService")
class StatementParserServiceTest {

    private final StatementParserService parser = new StatementParserService();

    @Test
    @DisplayName("riconosce un estratto conto PDF a tabella (data/descrizione/entrate/uscite/saldo su righe separate)")
    void parsesTableLayoutPdf() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("sample-statement-table.pdf")) {
            assertThat(in).isNotNull();
            List<RawTransaction> result = parser.parsePdf(in);

            // 22 righe di movimento nel PDF, esclusa la riga "Saldo iniziale" (solo saldo, nessun movimento)
            assertThat(result).hasSize(22);

            RawTransaction stipendio = result.stream().filter(t -> t.description().equals("Stipendio")).findFirst().orElseThrow();
            assertThat(stipendio.amount()).isEqualByComparingTo(new BigDecimal("2000.00"));

            RawTransaction mutuo = result.stream().filter(t -> t.description().equals("Mutuo casa")).findFirst().orElseThrow();
            assertThat(mutuo.amount()).isEqualByComparingTo(new BigDecimal("-750.00"));

            RawTransaction carburante = result.stream().filter(t -> t.description().equals("Carburante")).findFirst().orElseThrow();
            assertThat(carburante.amount()).isEqualByComparingTo(new BigDecimal("-70.00"));

            double totalIncome = result.stream().filter(t -> t.amount().signum() > 0).mapToDouble(t -> t.amount().doubleValue()).sum();
            double totalExpense = result.stream().filter(t -> t.amount().signum() < 0).mapToDouble(t -> -t.amount().doubleValue()).sum();
            assertThat(totalIncome).isEqualTo(2000.0);
            // Spese fisse + variabili (1955) + bonifico investimenti (200) + fondo emergenze (100):
            // a livello di parsing grezzo sono tutti addebiti, la categorizzazione avviene altrove.
            assertThat(totalExpense).isEqualTo(2255.0);
        }
    }

    @Test
    @DisplayName("riconosce un estratto conto PDF \"a riga\" con data completa e segno esplicito")
    void parsesSimpleSignedRowsPdf() throws Exception {
        byte[] pdf = buildPdf(List.of(
                "01/03/2024 Stipendio +1.500,00",
                "03/03/2024 Pagamento Netflix -12,99",
                "05/03/2024 Affitto -650,00"
        ));

        List<RawTransaction> result = parser.parsePdf(new ByteArrayInputStream(pdf));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).description()).isEqualTo("Stipendio");
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("1500.00")); // 1.500,00 formato italiano
        assertThat(result.get(1).description()).isEqualTo("Pagamento Netflix");
        assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("-12.99"));
        assertThat(result.get(2).amount()).isEqualByComparingTo(new BigDecimal("-650.00"));
    }

    @Test
    @DisplayName("riconosce importi senza separatore delle migliaia (es. \"1500,00\" invece di \"1.500,00\")")
    void parsesAmountsWithoutThousandsSeparator() throws Exception {
        byte[] pdf = buildPdf(List.of(
                "01/03/2024 Stipendio +1500,00",
                "05/03/2024 Affitto -650,00"
        ));

        List<RawTransaction> result = parser.parsePdf(new ByteArrayInputStream(pdf));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("-650.00"));
    }

    @Test
    @DisplayName("quando l'importo non ha il segno \"-\" lo considera un'entrata (stipendio, regalo, ecc.), non solo se la descrizione contiene parole chiave")
    void treatsUnsignedAmountsAsIncomeRegardlessOfDescription() throws Exception {
        byte[] pdf = buildPdf(List.of(
                "01/03/2024 Regalo zia Maria 200,00",
                "02/03/2024 Da Mario per cena 30,00",
                "05/03/2024 Affitto -650,00"
        ));

        List<RawTransaction> result = parser.parsePdf(new ByteArrayInputStream(pdf));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(result.get(2).amount()).isEqualByComparingTo(new BigDecimal("-650.00"));
    }

    @Test
    @DisplayName("riconosce un estratto conto \"a 5 colonne\" (es. Intesa Sanpaolo) anche quando descrizione e categoria vanno a capo su più righe")
    void parsesWideTableLayoutWithWrappedDescriptions() throws Exception {
        byte[] pdf = buildPdf(List.of(
                "DATA CONTABILE OPERAZIONE CONTABILIZZATO CATEGORIA IMPORTO",
                "26.06.2026 Trasferimento Denaro",
                "BANCOMAT Pay NO Addebiti vari € -10,00",
                "26.06.2026 Stipendio O Pensione SI Stipendi e",
                "pensioni € 2.935,00",
                "21.06.2026",
                "Bonifico istantaneo disposto da",
                "BARONI SILVIA",
                "SI Bonifici ricevuti € 20,00"
        ));

        List<RawTransaction> result = parser.parsePdf(new ByteArrayInputStream(pdf));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).description()).isEqualTo("Trasferimento Denaro BANCOMAT Pay");
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("-10.00"));
        assertThat(result.get(0).sourceCategory()).isEqualTo("Addebiti vari");
        assertThat(result.get(1).description()).isEqualTo("Stipendio O Pensione");
        assertThat(result.get(1).amount()).isEqualByComparingTo(new BigDecimal("2935.00"));
        assertThat(result.get(1).sourceCategory()).isEqualTo("Stipendi e pensioni");
        assertThat(result.get(2).description()).isEqualTo("Bonifico istantaneo disposto da BARONI SILVIA");
        assertThat(result.get(2).amount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.get(2).sourceCategory()).isEqualTo("Bonifici ricevuti");
    }

    private byte[] buildPdf(List<String> lines) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.setLeading(16f);
                cs.newLineAtOffset(50, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLine();
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }
}
