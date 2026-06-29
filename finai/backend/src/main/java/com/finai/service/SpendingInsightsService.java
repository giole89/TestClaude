package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.CategoryAmountDto;
import com.finai.dto.finance.SavingSuggestionDto;
import com.finai.dto.finance.SpendingInsightsDto;
import com.finai.repository.BankTransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analizza i movimenti reali importati dall'estratto conto per produrre
 * suggerimenti di risparmio concreti, ciascuno ancorato a numeri effettivi
 * (non consigli generici). Tre tipi di analisi:
 * <ul>
 *   <li><b>Quota su entrate</b>: categorie discrezionali che assorbono una
 *       fetta sproporzionata delle entrate stimate (ispirato alla regola
 *       50/30/20 — bisogni/desideri/risparmio);</li>
 *   <li><b>Spese ricorrenti</b>: pagamenti con descrizione e importo stabili
 *       ripetuti su più mesi (abbonamenti, canoni) da rivalutare;</li>
 *   <li><b>Trend in crescita</b>: categorie il cui ultimo mese concluso si è
 *       discostato molto in eccesso dalla media dei mesi precedenti.</li>
 * </ul>
 */
@Service
public class SpendingInsightsService {

    private static final int LOOKBACK_MONTHS = 6;
    private static final double DISCRETIONARY_SHARE_ALERT = 0.08; // 8% delle entrate su una sola categoria
    private static final double WANTS_SHARE_ALERT = 0.30;         // regola 50/30/20: max 30% in "desideri"
    private static final double TREND_INCREASE_ALERT = 0.30;      // +30% rispetto alla media
    private static final double TREND_MIN_ABS_INCREASE = 15.0;
    private static final int RECURRING_MIN_MONTHS = 3;
    private static final double RECURRING_AMOUNT_TOLERANCE = 0.15; // 15% di variazione tollerata

    private static final Set<String> DISCRETIONARY_CATEGORIES = Set.of(
            "Tempo libero", "Ristorazione", "Shopping", "Abbonamenti");

    private final BankTransactionRepository transactions;
    private final BudgetService budgetService;

    public SpendingInsightsService(BankTransactionRepository transactions, BudgetService budgetService) {
        this.transactions = transactions;
        this.budgetService = budgetService;
    }

    public SpendingInsightsDto analyze() {
        BudgetDto budget = budgetService.computeNextMonthBudget();
        List<BankTransaction> all = transactions.findAllByOrderByTxDateDesc();

        List<SavingSuggestionDto> suggestions = new ArrayList<>();
        suggestions.addAll(highShareSuggestions(budget));
        suggestions.addAll(recurringSuggestions(all));
        suggestions.addAll(trendSuggestions(all));

        suggestions.sort(Comparator.comparing(SavingSuggestionDto::potentialMonthlySaving, Comparator.nullsLast(Comparator.reverseOrder())));

        double total = suggestions.stream()
                .map(SavingSuggestionDto::potentialMonthlySaving)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        return new SpendingInsightsDto(suggestions, round(total));
    }

    // ─────────────────────────────── Quota su entrate ─────────────────────────

    private List<SavingSuggestionDto> highShareSuggestions(BudgetDto budget) {
        List<SavingSuggestionDto> result = new ArrayList<>();
        double income = budget.estimatedIncome() != null ? budget.estimatedIncome() : 0.0;
        if (income <= 0) return result;

        double discretionaryTotal = 0.0;
        for (CategoryAmountDto c : budget.variableByCategory()) {
            if (!DISCRETIONARY_CATEGORIES.contains(c.category())) continue;
            discretionaryTotal += c.amount();
            double share = c.amount() / income;
            if (share <= DISCRETIONARY_SHARE_ALERT) continue;

            double potential = round(c.amount() * 0.20);
            result.add(new SavingSuggestionDto(
                    "HIGH_SHARE", c.category(), share > 0.15 ? "ALTA" : "MEDIA",
                    "\"" + c.category() + "\" assorbe il " + pct(share) + " delle entrate",
                    String.format(Locale.ITALIAN,
                            "Spendi in media %.2f€/mese in %s, pari al %s delle entrate stimate (%.2f€). "
                                    + "Riducendo questa categoria del 20%% libereresti circa %.2f€/mese.",
                            c.amount(), c.category(), pct(share), income, potential),
                    potential));
        }

        if (discretionaryTotal / income > WANTS_SHARE_ALERT) {
            double excess = discretionaryTotal - income * WANTS_SHARE_ALERT;
            result.add(new SavingSuggestionDto(
                    "WANTS_OVER_BUDGET", null, "ALTA",
                    "Le spese discrezionali superano il 30% delle entrate",
                    String.format(Locale.ITALIAN,
                            "Le categorie discrezionali (tempo libero, ristorazione, shopping, abbonamenti) assorbono "
                                    + "in media %.2f€/mese, il %s delle entrate stimate (%.2f€). La regola 50/30/20 "
                                    + "consiglia di non superare il 30%%: per rientrare dovresti tagliare circa %.2f€/mese.",
                            discretionaryTotal, pct(discretionaryTotal / income), income, round(excess)),
                    round(excess)));
        }
        return result;
    }

    // ─────────────────────────────── Spese ricorrenti ─────────────────────────

    private List<SavingSuggestionDto> recurringSuggestions(List<BankTransaction> all) {
        record Occurrence(YearMonth month, BigDecimal amount, String category) {}

        Map<String, List<Occurrence>> byDescription = all.stream()
                .filter(t -> TransactionCategorizer.VARIABLE.equals(t.getType()))
                .collect(Collectors.groupingBy(t -> normalize(t.getDescription()),
                        Collectors.mapping(t -> new Occurrence(YearMonth.from(t.getTxDate()), t.getAmount().abs(), t.getCategory()),
                                Collectors.toList())));

        List<SavingSuggestionDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Occurrence>> e : byDescription.entrySet()) {
            Map<YearMonth, BigDecimal> perMonth = e.getValue().stream()
                    .collect(Collectors.toMap(Occurrence::month, Occurrence::amount, BigDecimal::add));
            if (perMonth.size() < RECURRING_MIN_MONTHS) continue;

            double avg = perMonth.values().stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            if (avg <= 0) continue;
            boolean stable = perMonth.values().stream()
                    .allMatch(v -> Math.abs(v.doubleValue() - avg) / avg <= RECURRING_AMOUNT_TOLERANCE);
            if (!stable) continue;

            String category = e.getValue().get(0).category();
            String label = e.getKey();
            result.add(new SavingSuggestionDto(
                    "RECURRING", category, "MEDIA",
                    "Pagamento ricorrente: " + label,
                    String.format(Locale.ITALIAN,
                            "\"%s\" (%s) si ripete da %d mesi a circa %.2f€/mese, per un totale di %.2f€/mese. "
                                    + "Verifica se lo usi ancora: se non serve più, disdirlo libera %.2f€/mese.",
                            label, category, perMonth.size(), avg, avg, avg),
                    round(avg)));
        }
        return result;
    }

    // ─────────────────────────────── Trend in crescita ─────────────────────────

    private List<SavingSuggestionDto> trendSuggestions(List<BankTransaction> all) {
        YearMonth currentMonth = YearMonth.now();
        List<YearMonth> completedMonths = all.stream()
                .map(t -> YearMonth.from(t.getTxDate()))
                .distinct()
                .filter(ym -> !ym.equals(currentMonth))
                .sorted(Comparator.reverseOrder())
                .limit(LOOKBACK_MONTHS)
                .toList();
        if (completedMonths.size() < 2) return List.of();

        YearMonth lastMonth = completedMonths.get(0);
        List<YearMonth> priorMonths = completedMonths.subList(1, completedMonths.size());

        Map<String, Double> lastMonthByCategory = sumByCategory(all, Set.of(lastMonth));
        Map<String, Double> priorAvgByCategory = avgByCategory(all, priorMonths);

        List<SavingSuggestionDto> result = new ArrayList<>();
        for (Map.Entry<String, Double> e : lastMonthByCategory.entrySet()) {
            double lastAmount = e.getValue();
            double priorAvg = priorAvgByCategory.getOrDefault(e.getKey(), 0.0);
            if (priorAvg <= 0) continue;
            double increase = lastAmount - priorAvg;
            double increasePct = increase / priorAvg;
            if (increasePct < TREND_INCREASE_ALERT || increase < TREND_MIN_ABS_INCREASE) continue;

            result.add(new SavingSuggestionDto(
                    "TREND_UP", e.getKey(), increasePct > 0.6 ? "ALTA" : "MEDIA",
                    "\"" + e.getKey() + "\" in forte aumento a " + monthLabel(lastMonth),
                    String.format(Locale.ITALIAN,
                            "A %s hai speso %.2f€ in %s, il %s in più rispetto alla media dei mesi precedenti (%.2f€). "
                                    + "Se non è una spesa eccezionale, rientrare alla media libererebbe circa %.2f€.",
                            monthLabel(lastMonth), lastAmount, e.getKey(), pct(increasePct), priorAvg, round(increase)),
                    round(increase)));
        }
        return result;
    }

    private Map<String, Double> sumByCategory(List<BankTransaction> all, Set<YearMonth> months) {
        return all.stream()
                .filter(t -> TransactionCategorizer.VARIABLE.equals(t.getType()))
                .filter(t -> months.contains(YearMonth.from(t.getTxDate())))
                .collect(Collectors.groupingBy(BankTransaction::getCategory,
                        Collectors.summingDouble(t -> t.getAmount().abs().doubleValue())));
    }

    private Map<String, Double> avgByCategory(List<BankTransaction> all, List<YearMonth> months) {
        if (months.isEmpty()) return Map.of();
        Map<String, Double> totals = sumByCategory(all, new HashSet<>(months));
        Map<String, Double> avg = new HashMap<>();
        totals.forEach((k, v) -> avg.put(k, v / months.size()));
        return avg;
    }

    private String normalize(String description) {
        return description == null ? "" : description.trim().toLowerCase(Locale.ITALIAN).replaceAll("\\s+", " ");
    }

    private String pct(double share) {
        return String.format(Locale.ITALIAN, "%.0f%%", share * 100);
    }

    private String monthLabel(YearMonth ym) {
        String label = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN));
        return label.substring(0, 1).toUpperCase(Locale.ITALIAN) + label.substring(1);
    }

    private double round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
