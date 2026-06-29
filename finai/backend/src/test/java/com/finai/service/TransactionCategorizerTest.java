package com.finai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionCategorizer")
class TransactionCategorizerTest {

    private final TransactionCategorizer categorizer = new TransactionCategorizer();

    @Test
    @DisplayName("riconosce entrate e uscite in base al segno dell'importo")
    void classifiesIncomeAndExpenseBySign() {
        assertThat(categorizer.classify("Stipendio", new BigDecimal("2000.00")).type())
                .isEqualTo(TransactionCategorizer.INCOME);
        assertThat(categorizer.classify("Mutuo casa", new BigDecimal("-750.00")).type())
                .isEqualTo(TransactionCategorizer.VARIABLE);
    }

    @Test
    @DisplayName("riconosce categorie specifiche da parole chiave nella descrizione")
    void classifiesSpecificCategories() {
        assertThat(categorizer.classify("Pagamento Esselunga", new BigDecimal("-45.30")).category())
                .isEqualTo("Alimentari");
        assertThat(categorizer.classify("Netflix.com", new BigDecimal("-12.99")).category())
                .isEqualTo("Abbonamenti");
        assertThat(categorizer.classify("Rimborso spese", new BigDecimal("150.00")).category())
                .isEqualTo("Rimborso");
    }

    @Test
    @DisplayName("applica una catalogazione generica più utile di \"Altro\" quando la descrizione contiene indizi (bonifico, POS, ecc.)")
    void appliesGenericHintsBeforeFallback() {
        assertThat(categorizer.classify("Bonifico a favore di terzi", new BigDecimal("-300.00")).category())
                .isEqualTo("Bonifici");
        assertThat(categorizer.classify("Pagamento POS negozio", new BigDecimal("-25.00")).category())
                .isEqualTo("Pagamenti con carta");
        assertThat(categorizer.classify("Accredito da terzi", new BigDecimal("100.00")).category())
                .isEqualTo("Accrediti");
    }

    @Test
    @DisplayName("distingue le spese sanitarie umane da quelle veterinarie")
    void distinguishesHealthFromVeterinaryExpenses() {
        assertThat(categorizer.classify("Farmacia Centrale", new BigDecimal("-18.50")).category())
                .isEqualTo("Salute");
        assertThat(categorizer.classify("Clinica Veterinaria San Rocco", new BigDecimal("-85.00")).category())
                .isEqualTo("Veterinario");
        assertThat(categorizer.classify("Ambulatorio Veterinario Dr. Bianchi", new BigDecimal("-60.00")).category())
                .isEqualTo("Veterinario");
    }

    @Test
    @DisplayName("quando l'estratto conto riporta un'etichetta generica (es. \"Altro\") ricade comunque sulla categoria di default in base al segno")
    void fallsBackToGenericCategoryForGenericSourceLabels() {
        assertThat(categorizer.classify("Altro", new BigDecimal("-10.00")))
                .isEqualTo(new TransactionCategorizer.Classification("Altro", TransactionCategorizer.VARIABLE));
        assertThat(categorizer.classify("Altro", new BigDecimal("10.00")))
                .isEqualTo(new TransactionCategorizer.Classification("Altra entrata", TransactionCategorizer.INCOME));
    }

    @Test
    @DisplayName("quando l'estratto conto fornisce già una categoria la usa direttamente, senza riprovare il riconoscimento per parole chiave")
    void usesSourceCategoryDirectlyWhenProvided() {
        assertThat(categorizer.classifyWithSourceCategory("Generi alimentari e supermercato", new BigDecimal("-33.64")))
                .isEqualTo(new TransactionCategorizer.Classification("Generi alimentari e supermercato", TransactionCategorizer.VARIABLE));
        assertThat(categorizer.classifyWithSourceCategory("Stipendi e pensioni", new BigDecimal("2935.00")))
                .isEqualTo(new TransactionCategorizer.Classification("Stipendi e pensioni", TransactionCategorizer.INCOME));
    }

    @Test
    @DisplayName("elenca le categorie note per tipo, incluso il fallback, per popolare un menu a tendina")
    void listsKnownCategoriesByType() {
        assertThat(categorizer.knownCategories(TransactionCategorizer.INCOME))
                .contains("Stipendio", "Pensione", "Bonifici ricevuti", "Altra entrata");
        assertThat(categorizer.knownCategories(TransactionCategorizer.VARIABLE))
                .contains("Alimentari", "Veterinario", "Salute", "Bonifici", "Altro");
    }
}
