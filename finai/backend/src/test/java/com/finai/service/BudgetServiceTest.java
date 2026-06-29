package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.domain.entity.FixedExpense;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.CategoryAmountDto;
import com.finai.dto.finance.MonthlyExpensesDto;
import com.finai.repository.BankTransactionRepository;
import com.finai.repository.FixedExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test unitari per {@link BudgetService}: ripartizione per categoria di entrate e spese.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService")
class BudgetServiceTest {

    @Mock
    private BankTransactionRepository transactionRepo;
    @Mock
    private FixedExpenseRepository fixedExpenseRepo;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(transactionRepo, fixedExpenseRepo);
        lenient().when(fixedExpenseRepo.findByActiveTrue()).thenReturn(List.of());
    }

    private BankTransaction tx(LocalDate date, String category, String type, String amount) {
        BankTransaction t = new BankTransaction();
        t.setId(java.util.UUID.randomUUID().toString());
        t.setTxDate(date);
        t.setDescription(category);
        t.setCategory(category);
        t.setType(type);
        t.setAmount(new BigDecimal(amount));
        return t;
    }

    @Test
    @DisplayName("calcola la ripartizione delle entrate per categoria, non solo il totale")
    void computesIncomeByCategory() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                tx(lastMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00"),
                tx(lastMonth, "Rimborso", TransactionCategorizer.INCOME, "100.00"),
                tx(lastMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-50.00")
        ));

        BudgetDto budget = service.computeNextMonthBudget();

        assertThat(budget.incomeByCategory()).extracting(CategoryAmountDto::category)
                .containsExactlyInAnyOrder("Stipendio", "Rimborso");
        assertThat(budget.incomeByCategory()).filteredOn(c -> c.category().equals("Stipendio"))
                .first().extracting(CategoryAmountDto::amount).isEqualTo(1500.00);
        assertThat(budget.estimatedIncome()).isEqualTo(1600.00);
    }

    @Test
    @DisplayName("esclude il mese in corso (ancora incompleto) dalla media storica, per non sottostimare la stima")
    void excludesCurrentInProgressMonthFromHistoricalAverage() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate inCurrentMonth = currentMonth.atDay(1);
        LocalDate lastMonth = currentMonth.minusMonths(1).atDay(1);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                // Mese in corso: tante spese già registrate, ma il mese non è ancora finito.
                tx(inCurrentMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-2000.00"),
                // Unico mese passato completo: la stima deve basarsi solo su questo.
                tx(lastMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-400.00"),
                tx(lastMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00")
        ));

        BudgetDto budget = service.computeNextMonthBudget();

        assertThat(budget.variableCostsEstimate()).isEqualTo(400.00);
        assertThat(budget.estimatedIncome()).isEqualTo(1500.00);
        assertThat(budget.monthsOfHistory()).isEqualTo(1);
    }

    @Test
    @DisplayName("usa il mese in corso come stima provvisoria quando non c'è ancora alcun mese passato completo")
    void fallsBackToCurrentMonthWhenNoCompletedHistoryExists() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate inCurrentMonth = currentMonth.atDay(1);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                tx(inCurrentMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-300.00"),
                tx(inCurrentMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00")
        ));

        BudgetDto budget = service.computeNextMonthBudget();

        assertThat(budget.estimatedIncome()).isEqualTo(1500.00);
        assertThat(budget.variableCostsEstimate()).isEqualTo(300.00);
        assertThat(budget.basedOnCurrentMonthOnly()).isTrue();
        assertThat(budget.hasEnoughData()).isFalse();
        assertThat(budget.monthsOfHistory()).isEqualTo(0);
    }

    @Test
    @DisplayName("ignora un mese passato senza alcuna entrata (transazione residua isolata) e ricade sul mese in corso")
    void ignoresCompletedMonthWithoutAnyIncome() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate inCurrentMonth = currentMonth.atDay(10);
        LocalDate staleMonth = currentMonth.minusMonths(3).atDay(15);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                // Mese passato "completo" ma con un'unica transazione residua, senza alcuna entrata:
                // non rappresenta un mese reale di attività e non deve essere usato come base di stima.
                tx(staleMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-5.11"),
                // Mese in corso: dati reali (entrate e spese) ma ancora in corso.
                tx(inCurrentMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-300.00"),
                tx(inCurrentMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00")
        ));

        BudgetDto budget = service.computeNextMonthBudget();

        assertThat(budget.estimatedIncome()).isEqualTo(1500.00);
        assertThat(budget.variableCostsEstimate()).isEqualTo(300.00);
        assertThat(budget.basedOnCurrentMonthOnly()).isTrue();
        assertThat(budget.hasEnoughData()).isFalse();
        assertThat(budget.monthsOfHistory()).isEqualTo(0);
    }

    @Test
    @DisplayName("calcola le spese effettive del mese corrente per categoria, non una media storica")
    void computesCurrentMonthExpensesByCategory() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate inMonth = currentMonth.atDay(5);
        LocalDate lastMonth = currentMonth.minusMonths(1).atDay(5);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                tx(inMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-60.00"),
                tx(inMonth, "Veterinario", TransactionCategorizer.VARIABLE, "-40.00"),
                tx(lastMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-999.00"),
                tx(inMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00")
        ));

        MonthlyExpensesDto result = service.computeCurrentMonthExpenses();

        assertThat(result.total()).isEqualTo(100.00);
        assertThat(result.byCategory()).extracting(CategoryAmountDto::category)
                .containsExactlyInAnyOrder("Alimentari", "Veterinario");
    }
}
