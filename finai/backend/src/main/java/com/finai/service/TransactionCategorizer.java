package com.finai.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Categorizzazione euristica dei movimenti importati, basata su parole
 * chiave nella descrizione. Non richiede ML: copre le categorie di spesa
 * più comuni per un estratto conto italiano.
 */
@Service
public class TransactionCategorizer {

    public static final String INCOME      = "INCOME";
    public static final String VARIABLE    = "VARIABLE_EXPENSE";

    private static final Map<String, Set<String>> EXPENSE_CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, Set<String>> INCOME_CATEGORIES = new LinkedHashMap<>();

    static {
        EXPENSE_CATEGORIES.put("Alimentari", Set.of(
                "supermercato", "esselunga", "conad", "coop", "carrefour", "lidl", "eurospin", "iper", "market", "pam", "despar"));
        EXPENSE_CATEGORIES.put("Trasporti", Set.of(
                "benzina", "carburante", "eni ", "esso", "autostrad", "telepass", "treno", "trenitalia", "italo",
                "atm ", "taxi", "uber", "parcheggio", "autobus", "metro"));
        EXPENSE_CATEGORIES.put("Casa e utenze", Set.of(
                "enel", "eni gas", "hera", "a2a", "acea", "condominio", "affitto", "mutuo", "tari", "iren",
                "fastweb", "tim ", "vodafone", "windtre", "wind ", "iliad", "gas", "luce", "acqua"));
        EXPENSE_CATEGORIES.put("Salute", Set.of(
                "farmacia", "parafarmacia", "medico", "dentista", "ospedale", "ticket sanitario", "ambulatorio"));
        EXPENSE_CATEGORIES.put("Abbonamenti", Set.of(
                "netflix", "spotify", "prime video", "disney+", "abbonamento", "subscription", "dazn", "now tv"));
        EXPENSE_CATEGORIES.put("Tempo libero", Set.of(
                "cinema", "teatro", "viaggio", "hotel", "booking", "ryanair", "easyjet", "airbnb", "palestra", "gym"));
        EXPENSE_CATEGORIES.put("Ristorazione", Set.of(
                "ristorante", "pizzeria", "just eat", "deliveroo", "glovo", "bar ", "trattoria", "osteria"));
        EXPENSE_CATEGORIES.put("Shopping", Set.of(
                "amazon", "zalando", "zara", "decathlon", "ikea", "mediaworld", "unieuro"));

        INCOME_CATEGORIES.put("Stipendio", Set.of("stipendio", "salary", "retribuzione"));
        INCOME_CATEGORIES.put("Pensione", Set.of("pensione", "inps"));
        INCOME_CATEGORIES.put("Rimborso", Set.of("rimborso", "refund", "storno"));
    }

    public record Classification(String category, String type) {}

    public Classification classify(String description, BigDecimal amount) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        boolean isIncome = amount != null && amount.signum() > 0;

        Map<String, Set<String>> dictionary = isIncome ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        for (Map.Entry<String, Set<String>> entry : dictionary.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (text.contains(keyword)) {
                    return new Classification(entry.getKey(), isIncome ? INCOME : VARIABLE);
                }
            }
        }
        return new Classification(isIncome ? "Altra entrata" : "Altro", isIncome ? INCOME : VARIABLE);
    }
}
