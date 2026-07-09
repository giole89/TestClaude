package com.finai.service;

import com.finai.domain.entity.FixedExpense;
import com.finai.domain.entity.InvestorProfile;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.mortgage.LoanRequest;
import com.finai.dto.finance.mortgage.LoanSimulationDto;
import com.finai.dto.finance.mortgage.MortgageRequest;
import com.finai.dto.finance.mortgage.MortgageSimulationDto;
import com.finai.exception.FinaiException;
import com.finai.repository.FixedExpenseRepository;
import com.finai.repository.InvestorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test unitari per {@link MortgageService}: calcolo rata, LTV, rapporto rata/reddito
 * (incluso il contributo di altri debiti già in essere), stress test tassi, costo
 * dell'acquisto non coperto dal mutuo e idoneità del fondo pensione.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MortgageService")
class MortgageServiceTest {

    @Mock
    private FixedExpenseRepository fixedExpenseRepo;
    @Mock
    private InvestorProfileRepository investorProfileRepo;
    @Mock
    private BudgetService budgetService;

    private MortgageService service;

    @BeforeEach
    void setUp() {
        service = new MortgageService(fixedExpenseRepo, investorProfileRepo, budgetService);
        lenient().when(fixedExpenseRepo.findByActiveTrue()).thenReturn(List.of());
        lenient().when(investorProfileRepo.findById("default")).thenReturn(Optional.empty());
        lenient().when(budgetService.computeNextMonthBudget()).thenReturn(budgetWithIncome(0.0, false));
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

    /** Costruisce una richiesta di mutuo con i soli campi obbligatori, tutto il resto null (costi/fondo pensione stimati). */
    private MortgageRequest basicRequest(double propertyValue, double loanAmount, double ratePct, int years, Double income) {
        return new MortgageRequest(propertyValue, loanAmount, ratePct, years, income,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("con tasso 0% la rata è semplicemente capitale/numero rate")
    void zeroRateMortgageIsFlatInstallment() {
        MortgageRequest req = basicRequest(200_000.0, 120_000.0, 0.0, 10, 2000.0);
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
        MortgageRequest highLtv = basicRequest(200_000.0, 180_000.0, 0.0, 10, 5000.0);
        MortgageRequest exactLtv = basicRequest(200_000.0, 160_000.0, 0.0, 10, 5000.0);

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

        MortgageRequest req = basicRequest(200_000.0, 24_000.0, 0.0, 1, 2000.0);
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

        MortgageRequest req = basicRequest(200_000.0, 120_000.0, 0.0, 10, null);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.monthlyNetIncome()).isEqualTo(3000.0);
        assertThat(result.incomeEstimated()).isTrue();
    }

    @Test
    @DisplayName("senza reddito dichiarato né stima disponibile, lancia un errore 422 esplicativo")
    void throwsWhenNoIncomeAvailable() {
        when(budgetService.computeNextMonthBudget()).thenReturn(budgetWithIncome(0.0, false));

        MortgageRequest req = basicRequest(200_000.0, 120_000.0, 3.0, 20, null);

        assertThatThrownBy(() -> service.simulateMortgage(req))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("Reddito netto mensile");
    }

    @Test
    @DisplayName("uno stress test di +2 punti percentuali può far superare la soglia di sostenibilità anche se la rata base è sotto soglia")
    void stressTestCanTipAffordabilityOverThreshold() {
        MortgageRequest req = basicRequest(300_000.0, 240_000.0, 3.0, 30, 3000.0);
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

    @Test
    @DisplayName("usa i costi accessori dichiarati quando forniti, senza segnarli come stimati")
    void usesDeclaredCostsWhenProvided() {
        MortgageRequest req = new MortgageRequest(200_000.0, 150_000.0, 3.0, 20, 3000.0,
                "PRIMA_CASA_PRIVATO", 2500.0, 800.0, 350.0, 6000.0, 3000.0, null, null, null);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.notaryCosts()).isEqualTo(2500.0);
        assertThat(result.notaryCostsEstimated()).isFalse();
        assertThat(result.originationFees()).isEqualTo(800.0);
        assertThat(result.originationFeesEstimated()).isFalse();
        assertThat(result.appraisalFees()).isEqualTo(350.0);
        assertThat(result.agencyFees()).isEqualTo(6000.0);
        assertThat(result.registrationTax()).isEqualTo(3000.0);
        assertThat(result.registrationTaxEstimated()).isFalse();

        assertThat(result.downPayment()).isEqualTo(50_000.0);
        assertThat(result.totalAncillaryCosts()).isEqualTo(2500.0 + 800.0 + 350.0 + 6000.0 + 3000.0);
        assertThat(result.totalOutOfPocketCost()).isEqualTo(result.downPayment() + result.totalAncillaryCosts());
    }

    @Test
    @DisplayName("senza costi dichiarati stima notaio, istruttoria, perizia, agenzia e imposta di registro (prima casa da privato)")
    void estimatesAncillaryCostsWhenNotDeclared() {
        MortgageRequest req = basicRequest(200_000.0, 150_000.0, 3.0, 20, 3000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.notaryCostsEstimated()).isTrue();
        assertThat(result.notaryCosts()).isEqualTo(4000.0); // 2% di 200.000
        assertThat(result.originationFeesEstimated()).isTrue();
        assertThat(result.originationFees()).isEqualTo(750.0); // 0.5% di 150.000
        assertThat(result.appraisalFeesEstimated()).isTrue();
        assertThat(result.appraisalFees()).isEqualTo(300.0); // flat
        assertThat(result.agencyFeesEstimated()).isTrue();
        assertThat(result.agencyFees()).isEqualTo(round2(200_000.0 * 0.03 * 1.22)); // 3% + IVA 22%
        assertThat(result.registrationTaxEstimated()).isTrue();
        assertThat(result.registrationTax()).isEqualTo(4000.0); // 2% di 200.000 (prima casa privato)
    }

    @Test
    @DisplayName("il fabbisogno residuo (shortfall) usa la liquidità dichiarata, altrimenti quella del profilo investitore")
    void shortfallUsesDeclaredThenProfileLiquidSavings() {
        MortgageRequest declared = new MortgageRequest(200_000.0, 180_000.0, 0.0, 10, 3000.0,
                "PRIMA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, 15_000.0, null, null);
        MortgageSimulationDto declaredResult = service.simulateMortgage(declared);
        assertThat(declaredResult.availableLiquidSavings()).isEqualTo(15_000.0);
        assertThat(declaredResult.liquidSavingsSource()).isEqualTo("DECLARED");
        assertThat(declaredResult.shortfall()).isEqualTo(declaredResult.totalOutOfPocketCost() - 15_000.0);

        InvestorProfile profile = new InvestorProfile();
        profile.setLiquidSavings(BigDecimal.valueOf(8_000.0));
        when(investorProfileRepo.findById("default")).thenReturn(Optional.of(profile));

        MortgageRequest fromProfile = new MortgageRequest(200_000.0, 180_000.0, 0.0, 10, 3000.0,
                "PRIMA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, null, null, null);
        MortgageSimulationDto profileResult = service.simulateMortgage(fromProfile);
        assertThat(profileResult.availableLiquidSavings()).isEqualTo(8_000.0);
        assertThat(profileResult.liquidSavingsSource()).isEqualTo("PROFILE");
    }

    @Test
    @DisplayName("con meno di 8 anni di iscrizione il fondo pensione non è idoneo per l'anticipazione prima casa")
    void pensionFundNotEligibleBeforeEightYears() {
        MortgageRequest req = new MortgageRequest(200_000.0, 150_000.0, 3.0, 20, 3000.0,
                "PRIMA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2, 10_000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.pensionFund()).isNotNull();
        assertThat(result.pensionFund().eligibleForHomePurchase()).isFalse();
        assertThat(result.pensionFund().yearsUntilEligible()).isEqualTo(6);
        assertThat(result.pensionFund().estimatedMaxAnticipation()).isNull();
        assertThat(result.pensionFund().note()).contains("8 anni");
    }

    @Test
    @DisplayName("con almeno 8 anni di iscrizione e prima casa il fondo pensione è idoneo e stima l'anticipazione al 75%")
    void pensionFundEligibleAfterEightYearsForFirstHome() {
        MortgageRequest req = new MortgageRequest(200_000.0, 150_000.0, 3.0, 20, 3000.0,
                "PRIMA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10, 40_000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.pensionFund().eligibleForHomePurchase()).isTrue();
        assertThat(result.pensionFund().maxAnticipationPct()).isEqualTo(75.0);
        assertThat(result.pensionFund().estimatedMaxAnticipation()).isEqualTo(30_000.0);
    }

    @Test
    @DisplayName("per la seconda casa il fondo pensione non è mai idoneo, anche con più di 8 anni di iscrizione")
    void pensionFundNeverEligibleForSecondHome() {
        MortgageRequest req = new MortgageRequest(200_000.0, 150_000.0, 3.0, 20, 3000.0,
                "SECONDA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 15, 40_000.0);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.pensionFund().eligibleForHomePurchase()).isFalse();
        assertThat(result.pensionFund().note()).contains("seconda casa");
    }

    @Test
    @DisplayName("l'elenco dei consigli di budget copre prima con la liquidità e poi propone il risparmio mensile residuo")
    void budgetAdviceOrdersLiquiditySavingsThenMonthlyPlan() {
        BudgetDto withInvestable = new BudgetDto("Agosto 2026", 3000.0, 500.0, 500.0, List.of(), List.of(), 500.0, 500.0, 3, true, false);
        when(budgetService.computeNextMonthBudget()).thenReturn(withInvestable);

        MortgageRequest req = new MortgageRequest(200_000.0, 150_000.0, 0.0, 10, 3000.0,
                "PRIMA_CASA_PRIVATO", 0.0, 0.0, 0.0, 0.0, 0.0, 10_000.0, null, null);
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.budgetAdvice()).isNotEmpty();
        assertThat(result.budgetAdvice().get(0).source()).isEqualTo("Liquidità disponibile");
        assertThat(result.budgetAdvice().stream().anyMatch(a -> a.source().equals("Risparmio mensile residuo"))).isTrue();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
