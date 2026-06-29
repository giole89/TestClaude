package com.finai.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
                "benzina", "carburante", "eni ", "esso", "q8", "ip ", "autostrad", "telepass", "treno", "trenitalia", "italo",
                "atm ", "taxi", "uber", "free now", "parcheggio", "autobus", "metro", "bicincitta", "noleggio auto", "car sharing"));
        EXPENSE_CATEGORIES.put("Casa e utenze", Set.of(
                "enel", "eni gas", "hera", "a2a", "acea", "condominio", "affitto", "mutuo", "tari", "iren",
                "fastweb", "tim ", "vodafone", "windtre", "wind ", "iliad", "gas", "luce", "acqua", "tigros", "imu", "rifiuti"));
        EXPENSE_CATEGORIES.put("Veterinario", Set.of(
                "veterinario", "veterinaria", "clinica veterinaria", "ambulatorio veterinario", "vet ",
                "petshop", "pet shop", "toelettatura", "mangimi", "croccantini"));
        EXPENSE_CATEGORIES.put("Salute", Set.of(
                "farmacia", "parafarmacia", "medico", "dentista", "ospedale", "ticket sanitario", "ambulatorio",
                "analisi", "studio medico", "fisioterapia", "psicologo", "oculista", "specialista"));
        EXPENSE_CATEGORIES.put("Abbonamenti", Set.of(
                "netflix", "spotify", "prime video", "disney+", "abbonamento", "subscription", "dazn", "now tv", "apple.com", "google play", "youtube premium"));
        EXPENSE_CATEGORIES.put("Tempo libero", Set.of(
                "cinema", "teatro", "viaggio", "hotel", "booking", "ryanair", "easyjet", "airbnb", "palestra", "gym", "trip.com", "expedia"));
        EXPENSE_CATEGORIES.put("Ristorazione", Set.of(
                "ristorante", "pizzeria", "just eat", "deliveroo", "glovo", "bar ", "trattoria", "osteria", "caffe", "caffetteria", "gelateria"));
        EXPENSE_CATEGORIES.put("Shopping", Set.of(
                "amazon", "zalando", "zara", "decathlon", "ikea", "mediaworld", "unieuro", "h&m", "vinted"));
        EXPENSE_CATEGORIES.put("Spese bancarie", Set.of(
                "commissione", "commissioni", "imposta di bollo", "canone conto", "spese tenuta conto", "competenze", "tenuta conto"));
        EXPENSE_CATEGORIES.put("Assicurazioni", Set.of(
                "assicurazione", "polizza", "generali", "allianz", "unipol"));
        EXPENSE_CATEGORIES.put("Tasse e imposte", Set.of(
                "f24", "agenzia delle entrate", "tributo", "tassa", "imposta", "inail"));
        EXPENSE_CATEGORIES.put("Risparmio e investimenti", Set.of(
                "investimento", "fondo comune", "deposito titoli", "fondo emergenze", "fondo pensione"));
        EXPENSE_CATEGORIES.put("Prelievi contante", Set.of(
                "prelievo", "bancomat", "cash"));

        INCOME_CATEGORIES.put("Stipendio", Set.of("stipendio", "salary", "retribuzione", "busta paga"));
        INCOME_CATEGORIES.put("Pensione", Set.of("pensione", "inps"));
        INCOME_CATEGORIES.put("Rimborso", Set.of("rimborso", "refund", "storno"));
        INCOME_CATEGORIES.put("Interessi e rendite", Set.of("interess", "dividend", "cedola"));
        INCOME_CATEGORIES.put("Bonifico ricevuto", Set.of(
                "bonifico in entrata", "bonifico a vostro favore", "bonifico ricevuto", "accredito bonifico"));
    }

    /** Categorie generiche dedotte dalla descrizione quando nessuna parola chiave specifica
     *  corrisponde: meglio di un indistinto "Altro" anche se non si individua il negozio/servizio. */
    private static final Map<String, Set<String>> GENERIC_EXPENSE_HINTS = new LinkedHashMap<>();
    private static final Map<String, Set<String>> GENERIC_INCOME_HINTS = new LinkedHashMap<>();

    static {
        GENERIC_EXPENSE_HINTS.put("Bonifici", Set.of("bonifico", "giroconto", "sepa"));
        GENERIC_EXPENSE_HINTS.put("Pagamenti con carta", Set.of("pos ", "pagamento carta", "carta di credito", "carta di debito"));
        GENERIC_EXPENSE_HINTS.put("Pagamenti online", Set.of("paypal", "satispay", "online"));
        GENERIC_EXPENSE_HINTS.put("Utenze e abbonamenti", Set.of("rid ", "domiciliazione", "sdd "));

        GENERIC_INCOME_HINTS.put("Bonifici ricevuti", Set.of("bonifico", "giroconto", "sepa"));
        GENERIC_INCOME_HINTS.put("Accrediti", Set.of("accredito", "versamento", "incasso"));
    }

    public record Classification(String category, String type) {}

    public Classification classify(String description, BigDecimal amount) {
        String text = description == null ? "" : description.toLowerCase(Locale.ROOT);
        boolean isIncome = amount != null && amount.signum() > 0;
        boolean genericLabel = isGenericLabel(text);

        // Se la descrizione è un'etichetta generica del file originale (es. "Altro"), la
        // categorizzazione si basa solo sull'importo/segno: non ci sono parole chiave da provare.
        if (!genericLabel) {
            Map<String, Set<String>> dictionary = isIncome ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
            for (Map.Entry<String, Set<String>> entry : dictionary.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (text.contains(keyword)) {
                        return new Classification(entry.getKey(), isIncome ? INCOME : VARIABLE);
                    }
                }
            }

            Map<String, Set<String>> genericHints = isIncome ? GENERIC_INCOME_HINTS : GENERIC_EXPENSE_HINTS;
            for (Map.Entry<String, Set<String>> entry : genericHints.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (text.contains(keyword)) {
                        return new Classification(entry.getKey(), isIncome ? INCOME : VARIABLE);
                    }
                }
            }
        }

        return new Classification(isIncome ? "Altra entrata" : "Altro", isIncome ? INCOME : VARIABLE);
    }

    /** Tutte le categorie note per il tipo indicato (specifiche + generiche + fallback), per popolare un menu a tendina. */
    public List<String> knownCategories(String type) {
        Set<String> names = new LinkedHashSet<>();
        if (INCOME.equals(type)) {
            names.addAll(INCOME_CATEGORIES.keySet());
            names.addAll(GENERIC_INCOME_HINTS.keySet());
            names.add("Altra entrata");
        } else {
            names.addAll(EXPENSE_CATEGORIES.keySet());
            names.addAll(GENERIC_EXPENSE_HINTS.keySet());
            names.add("Altro");
        }
        return List.copyOf(names);
    }

    /** Etichette generiche tipiche dei file di alcune banche, prive di informazione utile alla classificazione. */
    private boolean isGenericLabel(String text) {
        String trimmed = text.trim();
        return trimmed.equals("altro") || trimmed.equals("altri") || trimmed.equals("movimento")
                || trimmed.equals("varie") || trimmed.equals("n/d") || trimmed.isBlank();
    }
}
