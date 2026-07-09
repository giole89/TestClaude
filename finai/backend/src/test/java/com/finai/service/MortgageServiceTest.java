package com.finai.service;

import com.finai.domain.entity.FixedExpense;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.mortgage.LoanRequest;
import com.finai.dto.finance.mortgage.LoanSimulationDto;
import com.finai.dto.finance.mortgage.MortgageRequest;
import com.finai.dto.finance.mortgage.MortgageSimulationDto;
import com.finai.exception.FinaiException;
import com.finai.repository.FixedExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test unitari per {@link MortgageService}: calcolo rata, LTV, rapporto rata/reddito
 * (incluso il contributo di altri debiti già in essere) e stress test tassi.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MortgageService")
class MortgageServiceTest {

    @Mock
    private FixedExpenseRepository fixedExpenseRepo;
    @Mock
    private BudgetService budgetService;

    private MortgageService service;

    @BeforeEach
    void setUp() {
        service = new MortgageService(fixedExpenseRepo, budgetService);
        lenient().when(fixedExpenseRepo.findByActiveTrue()).thenReturn(List.of());
    }

    private FixedExpense debt(String name, double amount, double ratePct) {
        FixedExpense e = new FixedExpense();
        e.setId(java.util.UUID.randomUUID().toString());
        e.setName(name);
        e.setCategory("Altro");
        e.setAmount(BigDecimal.valueOf(amount));
        e.setActive(true);
        e.setInterestRatePct(BigDecimal.valueOf(ratePct));
        return e;
    }

    private BudgetDto budgetWithIncome(double income, boolean hasEnoughData) {
        return new BudgetDto("Agosto 2026", income, 0.0, 0.0, List.of(), List.of(), income, income, 3, hasEnoughData, false);
    }

    @Test
    @DisplayName("con tasso 0% la rata è semplicemente capitale/numero rate")
    void zeroRateMortgageIsFlatInstallment() {
        MortgageRequest req = new MortgageRequest(200_000.0, 120_000.0, 0.0, 10, 2000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.monthlyPayment()).isEqualTo(1000.0);
        assertThat(result.totalPaid()).isEqualTo(120_000.0);
        assertThat(result.totalInterest()).isEqualTo(0.0);
        assertThat(result.schedule()).hasSize(10);
        assertThat(result.schedule().get(9).remainingBalance()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("segnala LTV oltre l'80% ma non a esattamente l'80%")
    void flagsLtvAboveThresholdOnly() {
        MortgageRequest highLtv = new MortgageRequest(200_000.0, 180_000.0, 0.0, 10, 5000.0);
        MortgageRequest exactLtv = new MortgageRequest(200_000.0, 160_000.0, 0.0, 10, 5000.0);

        MortgageSimulationDto high = service.simulateMortgage(highLtv);
        MortgageSimulationDto exact = service.simulateMortgage(exactLtv);

        assertThat(high.loanToValuePct()).isEqualTo(90.0);
        assertThat(high.ltvWarning()).isNotNull();
        assertThat(exact.loanToValuePct()).isEqualTo(80.0);
        assertThat(exact.ltvWarning()).isNull();
    }

    @Test
    @DisplayName("il rapporto rata/reddito complessivo include le rate di altri debiti già tra le spese fisse")
    void combinedRatioIncludesOtherActiveDebts() {
        when(fixedExpenseRepo.findByActiveTrue()).thenReturn(List.of(debt("Prestito auto", 300.0, 7.0)));

        MortgageRequest req = new MortgageRequest(200_000.0, 24_000.0, 0.0, 1, 2000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.monthlyPayment()).isEqualTo(2000.0);
        assertThat(result.otherActiveDebtPayments()).isEqualTo(300.0);
        assertThat(result.paymentToIncomeRatioPct()).isEqualTo(100.0);
        assertThat(result.combinedPaymentToIncomeRatioPct()).isEqualTo(115.0);
        assertThat(result.affordabilityLabel()).isEqualTo("Rischioso");
        assertThat(result.affordabilityWarning()).isNotNull().contains("300");
    }

    @Test
    @DisplayName("senza reddito dichiarato usa la stima del budget e segnala che è stimato")
    void fallsBackToEstimatedIncomeWhenNotDeclared() {
        when(budgetService.computeNextMonthBudget()).thenReturn(budgetWithIncome(3000.0, true));

        MortgageRequest req = new MortgageRequest(200_000.0, 120_000.0, 0.0, 10, null);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.monthlyNetIncome()).isEqualTo(3000.0);
        assertThat(result.incomeEstimated()).isTrue();
    }

    @Test
    @DisplayName("senza reddito dichiarato né stima disponibile, lancia un errore 422 esplicativo")
    void throwsWhenNoIncomeAvailable() {
        when(budgetService.computeNextMonthBudget()).thenReturn(budgetWithIncome(0.0, false));

        MortgageRequest req = new MortgageRequest(200_000.0, 120_000.0, 3.0, 20, null);

        assertThatThrownBy(() -> service.simulateMortgage(req))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("Reddito netto mensile");
    }

    @Test
    @DisplayName("uno stress test di +2 punti percentuali può far superare la soglia di sostenibilità anche se la rata base è sotto soglia")
    void stressTestCanTipAffordabilityOverThreshold() {
        MortgageRequest req = new MortgageRequest(300_000.0, 240_000.0, 3.0, 30, 3000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.combinedPaymentToIncomeRatioPct()).isLessThan(35.0);
        assertThat(result.affordabilityWarning()).isNull();

        assertThat(result.stressTestRatePct()).isEqualTo(5.0);
        assertThat(result.stressTestMonthlyPayment()).isGreaterThan(result.monthlyPayment());
        assertThat(result.stressTestCombinedRatioPct()).isGreaterThan(35.0);
        assertThat(result.stressTestWarning()).isNotNull();
    }

    @Test
    @DisplayName("simula un finanziamento personale con lo stesso motore di calcolo della rata")
    void simulatesGenericLoan() {
        LoanRequest req = new LoanRequest(12_000.0, 0.0, 24, 1500.0);
        LoanSimulationDto result = service.simulateLoan(req);

        assertThat(result.monthlyPayment()).isEqualTo(500.0);
        assertThat(result.totalInterest()).isEqualTo(0.0);
        assertThat(result.paymentToIncomeRatioPct()).isCloseTo(33.33, within(0.1));
        assertThat(result.schedule()).hasSize(2);
    }
}
