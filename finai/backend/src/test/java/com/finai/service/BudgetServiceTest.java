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
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        when(transactionRepo.findAllByOrderByTxDateDesc()).thenReturn(List.of(
                tx(thisMonth, "Stipendio", TransactionCategorizer.INCOME, "1500.00"),
                tx(thisMonth, "Rimborso", TransactionCategorizer.INCOME, "100.00"),
                tx(thisMonth, "Alimentari", TransactionCategorizer.VARIABLE, "-50.00")
        ));

        BudgetDto budget = service.computeNextMonthBudget();

        assertThat(budget.incomeByCategory()).extracting(CategoryAmountDto::category)
                .containsExactlyInAnyOrder("Stipendio", "Rimborso");
        assertThat(budget.incomeByCategory()).filteredOn(c -> c.category().equals("Stipendio"))
                .first().extracting(CategoryAmountDto::amount).isEqualTo(1500.00);
        assertThat(budget.estimatedIncome()).isEqualTo(1600.00);
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
