package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.domain.entity.FixedExpense;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.CategoryAmountDto;
import com.finai.dto.finance.MonthlyExpensesDto;
import com.finai.repository.BankTransactionRepository;
import com.finai.repository.FixedExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcola il budget previsionale per il mese successivo combinando:
 * <ul>
 *   <li>costi fissi inseriti manualmente dall'utente ({@link FixedExpense});</li>
 *   <li>entrate e costi variabili stimati come media degli ultimi mesi
 *       importati da estratto conto ({@link BankTransaction}).</li>
 * </ul>
 * Il saldo previsionale ({@code projectedSavings}) è la differenza reale tra
 * entrate stimate e costi fissi+variabili previsti: può essere negativo, nel
 * qual caso il mese successivo è previsto in perdita. La quota investibile
 * ({@code investableAmount}) è invece sempre non negativa, perché non si può
 * investire più di quanto resti davvero disponibile.
 */
@Service
public class BudgetService {

    private static final int LOOKBACK_MONTHS = 6;

    private final BankTransactionRepository transactions;
    private final FixedExpenseRepository fixedExpenses;

    public BudgetService(BankTransactionRepository transactions, FixedExpenseRepository fixedExpenses) {
        this.transactions = transactions;
        this.fixedExpenses = fixedExpenses;
    }

    public BudgetDto computeNextMonthBudget() {
        List<BankTransaction> all = transactions.findAllByOrderByTxDateDesc();
        YearMonth currentMonth = YearMonth.now();

        // Il mese in corso non è ancora concluso: includerlo nella media storica la farebbe
        // risultare artificialmente più bassa (somma di un mese parziale divisa come se fosse
        // completo). La stima del prossimo mese si basa quindi sui mesi passati completi quando
        // disponibili; se non ce n'è ancora nessuno, o se quelli trovati non contengono nemmeno
        // un'entrata (es. una singola transazione residua isolata, non un mese di attività reale),
        // si ricade sul mese in corso piuttosto che produrre una stima palesemente inutile (entrate
        // a zero), segnalando comunque che si tratta di una stima provvisoria.
        List<YearMonth> completedMonths = all.stream()
                .map(t -> YearMonth.from(t.getTxDate()))
                .distinct()
                .filter(ym -> !ym.equals(currentMonth))
                .sorted(Comparator.reverseOrder())
                .limit(LOOKBACK_MONTHS)
                .toList();

        Set<YearMonth> completedMonthFilter = new HashSet<>(completedMonths);
        boolean completedMonthsHaveIncome = all.stream()
                .anyMatch(t -> completedMonthFilter.contains(YearMonth.from(t.getTxDate()))
                        && TransactionCategorizer.INCOME.equals(t.getType()));

        boolean basedOnCurrentMonthOnly = (completedMonths.isEmpty() || !completedMonthsHaveIncome)
                && all.stream().anyMatch(t -> YearMonth.from(t.getTxDate()).equals(currentMonth));
        List<YearMonth> recentMonths = basedOnCurrentMonthOnly ? List.of(currentMonth) : completedMonths;

        Set<YearMonth> monthFilter = new HashSet<>(recentMonths);
        int monthsCount = Math.max(recentMonths.size(), 1);

        List<BankTransaction> inWindow = all.stream()
                .filter(t -> monthFilter.contains(YearMonth.from(t.getTxDate())))
                .toList();

        BigDecimal totalIncome = sumByType(inWindow, TransactionCategorizer.INCOME).abs();
        BigDecimal totalVariable = sumByType(inWindow, TransactionCategorizer.VARIABLE).abs();

        Map<String, BigDecimal> byCategory = inWindow.stream()
                .filter(t -> TransactionCategorizer.VARIABLE.equals(t.getType()))
                .collect(Collectors.groupingBy(BankTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

        List<CategoryAmountDto> variableByCategory = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryAmountDto(e.getKey(), avg(e.getValue(), monthsCount)))
                .toList();

        Map<String, BigDecimal> incomeByCategoryTotals = inWindow.stream()
                .filter(t -> TransactionCategorizer.INCOME.equals(t.getType()))
                .collect(Collectors.groupingBy(BankTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

        List<CategoryAmountDto> incomeByCategory = incomeByCategoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryAmountDto(e.getKey(), avg(e.getValue(), monthsCount)))
                .toList();

        BigDecimal fixedCosts = fixedExpenses.findByActiveTrue().stream()
                .map(FixedExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double estimatedIncome = avg(totalIncome, monthsCount);
        double variableEstimate = avg(totalVariable, monthsCount);
        double fixed = round(fixedCosts).doubleValue();

        double balance = round(BigDecimal.valueOf(estimatedIncome - fixed - variableEstimate)).doubleValue();
        double investable = Math.max(0.0, balance);

        return new BudgetDto(
                nextMonthLabel(),
                estimatedIncome,
                fixed,
                variableEstimate,
                variableByCategory,
                incomeByCategory,
                balance,
                investable,
                basedOnCurrentMonthOnly ? 0 : completedMonths.size(),
                !basedOnCurrentMonthOnly && !completedMonths.isEmpty(),
                basedOnCurrentMonthOnly
        );
    }

    /** Spese variabili effettivamente sostenute nel mese corrente, per categoria (per il grafico a torta). */
    public MonthlyExpensesDto computeCurrentMonthExpenses() {
        YearMonth currentMonth = YearMonth.now();
        List<BankTransaction> inMonth = transactions.findAllByOrderByTxDateDesc().stream()
                .filter(t -> YearMonth.from(t.getTxDate()).equals(currentMonth))
                .filter(t -> TransactionCategorizer.VARIABLE.equals(t.getType()))
                .toList();

        Map<String, BigDecimal> byCategory = inMonth.stream()
                .collect(Collectors.groupingBy(BankTransaction::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, t -> t.getAmount().abs(), BigDecimal::add)));

        List<CategoryAmountDto> categories = byCategory.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new CategoryAmountDto(e.getKey(), round(e.getValue()).doubleValue()))
                .toList();

        double total = categories.stream().mapToDouble(CategoryAmountDto::amount).sum();
        return new MonthlyExpensesDto(currentMonthLabel(), round(BigDecimal.valueOf(total)).doubleValue(), categories);
    }

    private BigDecimal sumByType(List<BankTransaction> list, String type) {
        return list.stream()
                .filter(t -> type.equals(t.getType()))
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private double avg(BigDecimal total, int months) {
        return round(total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP)).doubleValue();
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String nextMonthLabel() {
        return monthLabel(LocalDate.now().plusMonths(1));
    }

    private String currentMonthLabel() {
        return monthLabel(LocalDate.now());
    }

    private String monthLabel(LocalDate date) {
        String label = date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALIAN));
        return label.substring(0, 1).toUpperCase(Locale.ITALIAN) + label.substring(1);
    }
}
