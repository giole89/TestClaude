package com.finai.service;

import com.finai.domain.entity.FixedExpense;
import com.finai.domain.entity.InvestorProfile;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.mortgage.HomeSaleRequest;
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
 * dell'acquisto non coperto dal mutuo, commissione di agenzia (% o importo fisso),
 * vendita di un immobile esistente e idoneità del fondo pensione.
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

    /** Builder locale per costruire {@link MortgageRequest} nei test senza dover elencare tutti i 16 campi posizionali ad ogni chiamata. */
    private static final class Req {
        double propertyValue, loanAmount, interestRatePct;
        int years;
        Double monthlyNetIncome;
        String purchaseType;
        Double notaryCosts, originationFees, appraisalFees, agencyFeePct, agencyFeeAmount, registrationTax, liquidSavings, pensionFundBalance;
        Integer pensionFundYears;
        HomeSaleRequest homeSale;

        static Req of(double propertyValue, double loanAmount, double interestRatePct, int years) {
            Req r = new Req();
            r.propertyValue = propertyValue;
            r.loanAmount = loanAmount;
            r.interestRatePct = interestRatePct;
            r.years = years;
            return r;
        }

        Req income(Double v) { this.monthlyNetIncome = v; return this; }
        Req purchaseType(String v) { this.purchaseType = v; return this; }
        Req notary(Double v) { this.notaryCosts = v; return this; }
        Req origination(Double v) { this.originationFees = v; return this; }
        Req appraisal(Double v) { this.appraisalFees = v; return this; }
        Req agencyPct(Double v) { this.agencyFeePct = v; return this; }
        Req agencyAmount(Double v) { this.agencyFeeAmount = v; return this; }
        Req registrationTax(Double v) { this.registrationTax = v; return this; }
        Req liquidSavings(Double v) { this.liquidSavings = v; return this; }
        Req pensionFund(Integer years, Double balance) { this.pensionFundYears = years; this.pensionFundBalance = balance; return this; }
        Req homeSale(HomeSaleRequest v) { this.homeSale = v; return this; }

        MortgageRequest build() {
            return new MortgageRequest(propertyValue, loanAmount, interestRatePct, years, monthlyNetIncome,
                    purchaseType, notaryCosts, originationFees, appraisalFees, agencyFeePct, agencyFeeAmount,
                    registrationTax, liquidSavings, pensionFundYears, pensionFundBalance, homeSale);
        }
    }

    @Test
    @DisplayName("con tasso 0% la rata è semplicemente capitale/numero rate")
    void zeroRateMortgageIsFlatInstallment() {
        MortgageRequest req = Req.of(200_000.0, 120_000.0, 0.0, 10).income(2000.0).build();
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
        MortgageRequest highLtv = Req.of(200_000.0, 180_000.0, 0.0, 10).income(5000.0).build();
        MortgageRequest exactLtv = Req.of(200_000.0, 160_000.0, 0.0, 10).income(5000.0).build();

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

        MortgageRequest req = Req.of(200_000.0, 24_000.0, 0.0, 1).income(2000.0).build();
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

        MortgageRequest req = Req.of(200_000.0, 120_000.0, 0.0, 10).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.monthlyNetIncome()).isEqualTo(3000.0);
        assertThat(result.incomeEstimated()).isTrue();
    }

    @Test
    @DisplayName("senza reddito dichiarato né stima disponibile, lancia un errore 422 esplicativo")
    void throwsWhenNoIncomeAvailable() {
        when(budgetService.computeNextMonthBudget()).thenReturn(budgetWithIncome(0.0, false));

        MortgageRequest req = Req.of(200_000.0, 120_000.0, 3.0, 20).build();

        assertThatThrownBy(() -> service.simulateMortgage(req))
                .isInstanceOf(FinaiException.class)
                .hasMessageContaining("Reddito netto mensile");
    }

    @Test
    @DisplayName("uno stress test di +2 punti percentuali può far superare la soglia di sostenibilità anche se la rata base è sotto soglia")
    void stressTestCanTipAffordabilityOverThreshold() {
        MortgageRequest req = Req.of(300_000.0, 240_000.0, 3.0, 30).income(3000.0).build();
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
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(2500.0).origination(800.0).appraisal(350.0).agencyAmount(6000.0).registrationTax(3000.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.notaryCosts()).isEqualTo(2500.0);
        assertThat(result.notaryCostsEstimated()).isFalse();
        assertThat(result.originationFees()).isEqualTo(800.0);
        assertThat(result.originationFeesEstimated()).isFalse();
        assertThat(result.appraisalFees()).isEqualTo(350.0);
        assertThat(result.agencyFees()).isEqualTo(6000.0);
        assertThat(result.agencyFeesEstimated()).isFalse();
        assertThat(result.registrationTax()).isEqualTo(3000.0);
        assertThat(result.registrationTaxEstimated()).isFalse();

        assertThat(result.downPayment()).isEqualTo(50_000.0);
        assertThat(result.totalAncillaryCosts()).isEqualTo(2500.0 + 800.0 + 350.0 + 6000.0 + 3000.0);
        assertThat(result.totalOutOfPocketCost()).isEqualTo(result.downPayment() + result.totalAncillaryCosts());
    }

    @Test
    @DisplayName("senza costi dichiarati stima notaio, istruttoria, perizia, agenzia (3%+IVA) e imposta di registro (prima casa da privato)")
    void estimatesAncillaryCostsWhenNotDeclared() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.notaryCostsEstimated()).isTrue();
        assertThat(result.notaryCosts()).isEqualTo(4000.0); // 2% di 200.000
        assertThat(result.originationFeesEstimated()).isTrue();
        assertThat(result.originationFees()).isEqualTo(750.0); // 0.5% di 150.000
        assertThat(result.appraisalFeesEstimated()).isTrue();
        assertThat(result.appraisalFees()).isEqualTo(300.0); // flat
        assertThat(result.agencyFeesEstimated()).isTrue();
        assertThat(result.agencyFeeMode()).isEqualTo("PERCENTAGE");
        assertThat(result.agencyFeesBase()).isEqualTo(6000.0); // 3% di 200.000
        assertThat(result.agencyFeesIva()).isEqualTo(1320.0); // 22% di 6000
        assertThat(result.agencyFees()).isEqualTo(7320.0);
        assertThat(result.registrationTaxEstimated()).isTrue();
        assertThat(result.registrationTax()).isEqualTo(4000.0); // 2% di 200.000 (prima casa privato)
    }

    @Test
    @DisplayName("commissione agenzia in percentuale: aggiunge automaticamente l'IVA al 22%")
    void agencyFeeFromPercentageAddsIvaAutomatically() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0).agencyPct(3.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.agencyFeeMode()).isEqualTo("PERCENTAGE");
        assertThat(result.agencyFeesEstimated()).isFalse();
        assertThat(result.agencyFeesBase()).isEqualTo(6000.0);
        assertThat(result.agencyFeesIva()).isEqualTo(1320.0);
        assertThat(result.agencyFees()).isEqualTo(7320.0);
    }

    @Test
    @DisplayName("commissione agenzia in euro: nessuna IVA aggiuntiva, l'importo è considerato già finale")
    void agencyFeeFromFlatAmountHasNoAdditionalIva() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0).agencyAmount(5000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.agencyFeeMode()).isEqualTo("AMOUNT");
        assertThat(result.agencyFeesEstimated()).isFalse();
        assertThat(result.agencyFeesBase()).isEqualTo(5000.0);
        assertThat(result.agencyFeesIva()).isEqualTo(0.0);
        assertThat(result.agencyFees()).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("se dichiarati entrambi, la percentuale ha priorità sull'importo fisso per la commissione di agenzia")
    void agencyFeePercentageTakesPriorityOverAmount() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .agencyPct(2.0).agencyAmount(9999.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.agencyFeeMode()).isEqualTo("PERCENTAGE");
        assertThat(result.agencyFeesBase()).isEqualTo(4000.0); // 2% di 200.000
    }

    @Test
    @DisplayName("il fabbisogno residuo (shortfall) usa la liquidità dichiarata, altrimenti quella del profilo investitore")
    void shortfallUsesDeclaredThenProfileLiquidSavings() {
        MortgageRequest declared = Req.of(200_000.0, 180_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .liquidSavings(15_000.0).build();
        MortgageSimulationDto declaredResult = service.simulateMortgage(declared);
        assertThat(declaredResult.availableLiquidSavings()).isEqualTo(15_000.0);
        assertThat(declaredResult.liquidSavingsSource()).isEqualTo("DECLARED");
        assertThat(declaredResult.shortfall()).isEqualTo(declaredResult.totalOutOfPocketCost() - 15_000.0);

        InvestorProfile profile = new InvestorProfile();
        profile.setLiquidSavings(BigDecimal.valueOf(8_000.0));
        when(investorProfileRepo.findById("default")).thenReturn(Optional.of(profile));

        MortgageRequest fromProfile = Req.of(200_000.0, 180_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto profileResult = service.simulateMortgage(fromProfile);
        assertThat(profileResult.availableLiquidSavings()).isEqualTo(8_000.0);
        assertThat(profileResult.liquidSavingsSource()).isEqualTo("PROFILE");
    }

    @Test
    @DisplayName("con meno di 8 anni di iscrizione il fondo pensione non è idoneo per l'anticipazione prima casa")
    void pensionFundNotEligibleBeforeEightYears() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .pensionFund(2, 10_000.0).build();
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
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .pensionFund(10, 40_000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.pensionFund().eligibleForHomePurchase()).isTrue();
        assertThat(result.pensionFund().maxAnticipationPct()).isEqualTo(75.0);
        assertThat(result.pensionFund().estimatedMaxAnticipation()).isEqualTo(30_000.0);
    }

    @Test
    @DisplayName("per la seconda casa il fondo pensione non è mai idoneo, anche con più di 8 anni di iscrizione")
    void pensionFundNeverEligibleForSecondHome() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("SECONDA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .pensionFund(15, 40_000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.pensionFund().eligibleForHomePurchase()).isFalse();
        assertThat(result.pensionFund().note()).contains("seconda casa");
    }

    @Test
    @DisplayName("l'elenco dei consigli di budget copre prima con la liquidità e poi propone il risparmio mensile residuo")
    void budgetAdviceOrdersLiquiditySavingsThenMonthlyPlan() {
        BudgetDto withInvestable = new BudgetDto("Agosto 2026", 3000.0, 500.0, 500.0, List.of(), List.of(), 500.0, 500.0, 3, true, false);
        when(budgetService.computeNextMonthBudget()).thenReturn(withInvestable);

        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .liquidSavings(10_000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.budgetAdvice()).isNotEmpty();
        assertThat(result.budgetAdvice().get(0).source()).isEqualTo("Liquidità disponibile");
        assertThat(result.budgetAdvice().stream().anyMatch(a -> a.source().equals("Risparmio mensile residuo"))).isTrue();
    }

    @Test
    @DisplayName("la plusvalenza da vendita di una casa posseduta da meno di 5 anni e non abitazione principale è tassata al 26%")
    void homeSaleCapitalGainsTaxableWhenRecentAndNotMainResidence() {
        HomeSaleRequest sale = new HomeSaleRequest(300_000.0, 200_000.0, 3, false, 0.0, null, 0.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale()).isNotNull();
        assertThat(result.homeSale().capitalGain()).isEqualTo(100_000.0);
        assertThat(result.homeSale().capitalGainsTaxable()).isTrue();
        assertThat(result.homeSale().capitalGainsTax()).isEqualTo(26_000.0);
    }

    @Test
    @DisplayName("la plusvalenza è esente se l'immobile è stato abitazione principale, anche se venduto prima di 5 anni")
    void homeSaleCapitalGainsExemptWhenMainResidence() {
        HomeSaleRequest sale = new HomeSaleRequest(300_000.0, 200_000.0, 2, true, 0.0, null, 0.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().capitalGainsTaxable()).isFalse();
        assertThat(result.homeSale().capitalGainsTax()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("la plusvalenza è esente dopo 5 anni di possesso, anche senza abitazione principale")
    void homeSaleCapitalGainsExemptAfterFiveYears() {
        HomeSaleRequest sale = new HomeSaleRequest(300_000.0, 200_000.0, 6, false, 0.0, null, 0.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().capitalGainsTaxable()).isFalse();
    }

    @Test
    @DisplayName("il capitale netto dalla vendita sottrae mutuo residuo e spese di agenzia, e riduce il fabbisogno del mutuo")
    void homeSaleNetProceedsReduceShortfall() {
        HomeSaleRequest sale = new HomeSaleRequest(200_000.0, 150_000.0, 10, true, 50_000.0, null, 5_000.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        // netProceeds = 200.000 - 5.000 (agenzia) - 50.000 (mutuo residuo) - 0 (esente, abitazione principale) = 145.000
        assertThat(result.homeSale().netProceeds()).isEqualTo(145_000.0);
        assertThat(result.totalAvailableCapital()).isEqualTo(145_000.0); // nessuna liquidità dichiarata
        assertThat(result.shortfall()).isEqualTo(Math.max(0, result.totalOutOfPocketCost() - 145_000.0));
        assertThat(result.budgetAdvice().get(0).source()).isEqualTo("Vendita immobile esistente");
    }

    @Test
    @DisplayName("se mutuo residuo e spese superano il valore di vendita, la vendita non libera capitale e lo segnala")
    void homeSaleNegativeNetProceedsIsFlagged() {
        HomeSaleRequest sale = new HomeSaleRequest(100_000.0, 90_000.0, 10, true, 110_000.0, null, 0.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().netProceeds()).isLessThan(0);
        assertThat(result.totalAvailableCapital()).isEqualTo(0.0);
        assertThat(result.budgetAdvice().stream().anyMatch(a -> a.source().equals("Vendita immobile esistente") && a.amount() == 0.0)).isTrue();
    }

    @Test
    @DisplayName("se i tempi di vendita previsti sono più corti della media, segnala il rischio di tempistica")
    void homeSaleTimingNoteWarnsWhenFasterThanAverage() {
        HomeSaleRequest sale = new HomeSaleRequest(200_000.0, 150_000.0, 10, true, 0.0, null, 0.0, 2, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().timingNote()).isNotNull().contains("mutuo ponte");
    }

    @Test
    @DisplayName("quando il reddito è il vincolo più stretto, il mutuo massimo consigliato è quello calcolato dal reddito, non dall'LTV")
    void maxLoanAdviceBindsOnIncomeWhenTighterThanLtv() {
        // Immobile costoso (LTV 80% = 800.000€) ma reddito modesto: la rata sostenibile implica un mutuo ben inferiore.
        MortgageRequest req = Req.of(1_000_000.0, 150_000.0, 3.0, 20).income(2000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.maxLoanAdvice()).isNotNull();
        assertThat(result.maxLoanAdvice().maxLoanByLtv()).isEqualTo(800_000.0);
        assertThat(result.maxLoanAdvice().maxLoanAtLimit()).isLessThan(result.maxLoanAdvice().maxLoanByLtv());
        assertThat(result.maxLoanAdvice().bindingConstraint()).isEqualTo("REDDITO");
        assertThat(result.maxLoanAdvice().recommendedMaxLoan()).isEqualTo(result.maxLoanAdvice().maxLoanAtLimit());
    }

    @Test
    @DisplayName("quando l'LTV è il vincolo più stretto, il mutuo massimo consigliato è l'80% del valore dell'immobile")
    void maxLoanAdviceBindsOnLtvWhenTighterThanIncome() {
        // Reddito molto alto (rata sostenibile enorme) ma immobile economico: l'LTV all'80% diventa il vincolo.
        MortgageRequest req = Req.of(100_000.0, 50_000.0, 3.0, 20).income(20_000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.maxLoanAdvice().maxLoanByLtv()).isEqualTo(80_000.0);
        assertThat(result.maxLoanAdvice().maxLoanAtLimit()).isGreaterThan(result.maxLoanAdvice().maxLoanByLtv());
        assertThat(result.maxLoanAdvice().bindingConstraint()).isEqualTo("LTV");
        assertThat(result.maxLoanAdvice().recommendedMaxLoan()).isEqualTo(80_000.0);
    }

    @Test
    @DisplayName("il mutuo richiesto viene confrontato correttamente col massimo consigliato, sopra e sotto soglia")
    void maxLoanAdviceComparesRequestedAmountCorrectly() {
        MortgageRequest belowMax = Req.of(1_000_000.0, 50_000.0, 3.0, 20).income(2000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto belowResult = service.simulateMortgage(belowMax);
        assertThat(belowResult.maxLoanAdvice().requestedLoanAmount()).isEqualTo(50_000.0);
        assertThat(belowResult.maxLoanAdvice().requestedLoanNote()).contains("entro il massimo consigliato");

        MortgageRequest aboveMax = Req.of(1_000_000.0, 900_000.0, 3.0, 20).income(2000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto aboveResult = service.simulateMortgage(aboveMax);
        assertThat(aboveResult.maxLoanAdvice().requestedLoanNote()).contains("supera di");
    }

    @Test
    @DisplayName("il mutuo minimo necessario dato il capitale disponibile è 0 se la liquidità copre già tutto il costo")
    void minLoanNeededIsZeroWhenCapitalCoversEverything() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .liquidSavings(300_000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.maxLoanAdvice().minLoanNeededGivenCapital()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("il mutuo minimo necessario riflette il capitale insufficiente a coprire prezzo e spese accessorie")
    void minLoanNeededReflectsInsufficientCapital() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .liquidSavings(10_000.0).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        // minLoanNeeded = propertyValue + totalAncillaryCosts(0) - capitale disponibile = 200.000 - 10.000 = 190.000
        assertThat(result.maxLoanAdvice().minLoanNeededGivenCapital()).isEqualTo(190_000.0);
    }

    @Test
    @DisplayName("il confronto tra durate include le durate tipiche e quella selezionata, con rata decrescente all'aumentare degli anni")
    void durationComparisonIncludesTypicalDurationsAndSelected() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 22).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        List<Integer> years = result.durationComparison().stream().map(d -> d.years()).toList();
        assertThat(years).containsExactly(10, 15, 20, 22, 25, 30);

        assertThat(result.durationComparison().stream().filter(d -> d.years() == 22).findFirst().orElseThrow().isSelected()).isTrue();
        assertThat(result.durationComparison().stream().filter(d -> d.years() == 10).findFirst().orElseThrow().isSelected()).isFalse();

        double payment10y = result.durationComparison().stream().filter(d -> d.years() == 10).findFirst().orElseThrow().monthlyPayment();
        double payment30y = result.durationComparison().stream().filter(d -> d.years() == 30).findFirst().orElseThrow().monthlyPayment();
        double interest10y = result.durationComparison().stream().filter(d -> d.years() == 10).findFirst().orElseThrow().totalInterest();
        double interest30y = result.durationComparison().stream().filter(d -> d.years() == 30).findFirst().orElseThrow().totalInterest();

        assertThat(payment10y).isGreaterThan(payment30y); // rata più alta su durata più corta
        assertThat(interest10y).isLessThan(interest30y);  // ma interessi totali più bassi
    }

    @Test
    @DisplayName("il confronto tra durate non duplica la durata selezionata se già tra quelle tipiche")
    void durationComparisonDoesNotDuplicateSelectedWhenAlreadyTypical() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        List<Integer> years = result.durationComparison().stream().map(d -> d.years()).toList();
        assertThat(years).containsExactly(10, 15, 20, 25, 30);
        assertThat(result.durationComparison().stream().filter(d -> d.years() == 20).findFirst().orElseThrow().isSelected()).isTrue();
    }

    @Test
    @DisplayName("quando il capitale netto della vendita copre da solo anticipo e spese, coversFullPurchase è true e non genera avvisi extra")
    void homeSaleSufficientWhenMustFullyFund() {
        // netProceeds = 300.000 - 0 (agenzia) - 0 (residuo) - 0 (esente, abitazione principale) = 300.000
        // totalOutOfPocketCost = downPayment(200.000-150.000=50.000) + costi accessori (0) = 50.000 → ampiamente coperto
        HomeSaleRequest sale = new HomeSaleRequest(300_000.0, 200_000.0, 10, true, 0.0, null, 0.0, null, true);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().mustFullyFundPurchase()).isTrue();
        assertThat(result.homeSale().coversFullPurchase()).isTrue();
        assertThat(result.homeSale().fundingGapOrSurplus()).isGreaterThan(0);
        assertThat(result.homeSale().fullFundingNote()).isNull();
        assertThat(result.budgetAdvice().stream().noneMatch(a -> a.source().contains("Vendita insufficiente"))).isTrue();
    }

    @Test
    @DisplayName("quando il capitale netto della vendita non basta da solo, coversFullPurchase è false e genera un avviso con alternative concrete")
    void homeSaleInsufficientWhenMustFullyFund() {
        // netProceeds = 120.000 - 0 - 0 - 0 = 120.000; totalOutOfPocketCost = downPayment(200.000-150.000=50.000) + 0 = 50.000
        // Per rendere il fabbisogno più alto del netProceeds, usiamo un mutuo molto più basso: downPayment enorme.
        HomeSaleRequest sale = new HomeSaleRequest(120_000.0, 100_000.0, 10, true, 0.0, null, 0.0, null, true);
        MortgageRequest req = Req.of(200_000.0, 20_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        // totalOutOfPocketCost = downPayment (200.000-20.000=180.000) + 0 costi = 180.000 > netProceeds 120.000
        assertThat(result.homeSale().mustFullyFundPurchase()).isTrue();
        assertThat(result.homeSale().coversFullPurchase()).isFalse();
        assertThat(result.homeSale().fundingGapOrSurplus()).isLessThan(0);
        assertThat(result.homeSale().fullFundingNote()).isNotNull().contains("unica fonte di capitale");
        assertThat(result.budgetAdvice().stream().anyMatch(a -> a.source().contains("Vendita insufficiente"))).isTrue();
    }

    @Test
    @DisplayName("senza il flag mustFullyFundPurchase, anche se il capitale netto non basta non viene generato l'avviso dedicato")
    void noFullFundingNoteWhenFlagNotSet() {
        HomeSaleRequest sale = new HomeSaleRequest(120_000.0, 100_000.0, 10, true, 0.0, null, 0.0, null, false);
        MortgageRequest req = Req.of(200_000.0, 20_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().mustFullyFundPurchase()).isFalse();
        assertThat(result.homeSale().coversFullPurchase()).isFalse();
        assertThat(result.homeSale().fullFundingNote()).isNull();
        assertThat(result.budgetAdvice().stream().noneMatch(a -> a.source().contains("Vendita insufficiente"))).isTrue();
    }

    @Test
    @DisplayName("commissione agenzia per la vendita in percentuale: aggiunge automaticamente l'IVA al 22%")
    void saleAgencyFeeFromPercentageAddsIvaAutomatically() {
        HomeSaleRequest sale = new HomeSaleRequest(200_000.0, 150_000.0, 10, true, 0.0, 3.0, null, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().saleAgencyFeeMode()).isEqualTo("PERCENTAGE");
        assertThat(result.homeSale().saleAgencyFeesEstimated()).isFalse();
        assertThat(result.homeSale().saleAgencyFeesBase()).isEqualTo(6_000.0); // 3% di 200.000
        assertThat(result.homeSale().saleAgencyFeesIva()).isEqualTo(1_320.0); // 22% di 6.000
        assertThat(result.homeSale().saleAgencyFees()).isEqualTo(7_320.0);
    }

    @Test
    @DisplayName("commissione agenzia per la vendita in euro: nessuna IVA aggiuntiva, importo già finale")
    void saleAgencyFeeFromFlatAmountHasNoAdditionalIva() {
        HomeSaleRequest sale = new HomeSaleRequest(200_000.0, 150_000.0, 10, true, 0.0, null, 5_000.0, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().saleAgencyFeeMode()).isEqualTo("AMOUNT");
        assertThat(result.homeSale().saleAgencyFeesEstimated()).isFalse();
        assertThat(result.homeSale().saleAgencyFeesIva()).isEqualTo(0.0);
        assertThat(result.homeSale().saleAgencyFees()).isEqualTo(5_000.0);
    }

    @Test
    @DisplayName("senza costi di agenzia dichiarati per la vendita, stima 3%+IVA del valore di vendita")
    void saleAgencyFeeEstimatedWhenNotDeclared() {
        HomeSaleRequest sale = new HomeSaleRequest(200_000.0, 150_000.0, 10, true, 0.0, null, null, null, null);
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 0.0, 10).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(0.0).registrationTax(0.0)
                .homeSale(sale).build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.homeSale().saleAgencyFeesEstimated()).isTrue();
        assertThat(result.homeSale().saleAgencyFeeMode()).isEqualTo("PERCENTAGE");
        assertThat(result.homeSale().saleAgencyFees()).isEqualTo(round2(200_000.0 * 0.03 * 1.22));
    }

    @Test
    @DisplayName("per la prima casa stima la detrazione IRPEF 19% su interessi (fino a 4.000€/anno) e spese di agenzia (fino a 1.000€)")
    void taxDeductionEligibleForFirstHome() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("PRIMA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(2000.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.taxDeductions()).isNotNull();
        assertThat(result.taxDeductions().eligible()).isTrue();
        assertThat(result.taxDeductions().interestDeductionRatePct()).isEqualTo(19.0);
        assertThat(result.taxDeductions().maxDeductibleInterestPerYear()).isEqualTo(4000.0);
        // interessi primo anno < 4000 in questo scenario: la detrazione è 19% degli interessi reali del primo anno
        double firstYearInterest = result.schedule().get(0).interestPaid();
        assertThat(result.taxDeductions().estimatedFirstYearInterest()).isEqualTo(firstYearInterest);
        assertThat(result.taxDeductions().estimatedAnnualInterestDeduction())
                .isEqualTo(round2(Math.min(firstYearInterest, 4000.0) * 0.19));
        // spesa agenzia 2000€ > 1000€ massimo deducibile: base capped a 1000€
        assertThat(result.taxDeductions().estimatedAgencyFeeDeduction()).isEqualTo(190.0); // 19% di 1000
    }

    @Test
    @DisplayName("per la seconda casa le detrazioni non sono ammesse")
    void taxDeductionNotEligibleForSecondHome() {
        MortgageRequest req = Req.of(200_000.0, 150_000.0, 3.0, 20).income(3000.0)
                .purchaseType("SECONDA_CASA_PRIVATO")
                .notary(0.0).origination(0.0).appraisal(0.0).agencyAmount(2000.0).registrationTax(0.0)
                .build();
        MortgageSimulationDto result = service.simulateMortgage(req);

        assertThat(result.taxDeductions().eligible()).isFalse();
        assertThat(result.taxDeductions().estimatedAnnualInterestDeduction()).isEqualTo(0.0);
        assertThat(result.taxDeductions().estimatedAgencyFeeDeduction()).isEqualTo(0.0);
        assertThat(result.taxDeductions().note()).contains("seconda casa");
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
