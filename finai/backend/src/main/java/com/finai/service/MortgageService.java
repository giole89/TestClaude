package com.finai.service;

import com.finai.domain.entity.FixedExpense;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.mortgage.*;
import com.finai.exception.FinaiException;
import com.finai.repository.FixedExpenseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calcolatore di mutui e finanziamenti: rata con piano di ammortamento alla francese (quota
 * capitale crescente, rata costante — lo standard dei mutui e prestiti personali italiani),
 * più gli indicatori di sostenibilità che una banca valuterebbe in fase di istruttoria:
 * <ul>
 *   <li>LTV (loan-to-value): quota del valore dell'immobile coperta dal mutuo richiesto;</li>
 *   <li>rapporto rata/reddito, calcolato non solo sulla nuova rata ma sommandola alle rate di
 *       eventuali altri debiti già tra le spese fisse dell'utente (il vero indicatore di
 *       sostenibilità usato in istruttoria è il rapporto rata complessiva/reddito, non la
 *       singola rata isolata);</li>
 *   <li>stress test: cosa succede alla sostenibilità se il tasso salisse, rilevante soprattutto
 *       per i mutui a tasso variabile;</li>
 *   <li>stima indicativa delle spese accessorie di un mutuo (notaio, imposte, perizia, istruttoria).</li>
 * </ul>
 */
@Service
public class MortgageService {

    /** LTV oltre il quale un mutuo fondiario italiano tipicamente richiede condizioni più severe o garanzie aggiuntive. */
    private static final double MAX_RECOMMENDED_LTV_PCT = 80.0;
    /** Rapporto rata complessiva/reddito oltre il quale la sostenibilità è considerata a rischio (prassi bancaria, linee guida Banca d'Italia). */
    private static final double MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT = 35.0;
    /** Soglia sotto la quale il rapporto è considerato pienamente sostenibile senza riserve. */
    private static final double COMFORTABLE_PAYMENT_TO_INCOME_PCT = 30.0;
    /** Rialzo di tasso simulato nello stress test (rilevante per mutui/finanziamenti a tasso variabile). */
    private static final double STRESS_TEST_RATE_INCREASE_PCT = 2.0;
    /** Stima indicativa delle spese accessorie di un mutuo (notaio, imposte, perizia, istruttoria) come % del valore immobile. */
    private static final double ANCILLARY_COSTS_PCT_OF_PROPERTY = 2.5;

    private final FixedExpenseRepository fixedExpenseRepo;
    private final BudgetService budgetService;

    public MortgageService(FixedExpenseRepository fixedExpenseRepo, BudgetService budgetService) {
        this.fixedExpenseRepo = fixedExpenseRepo;
        this.budgetService = budgetService;
    }

    public MortgageSimulationDto simulateMortgage(MortgageRequest req) {
        int months = req.years() * 12;
        double monthlyPayment = calcMonthlyPayment(req.loanAmount(), req.interestRatePct(), months);
        double totalPaid = monthlyPayment * months;
        double totalInterest = totalPaid - req.loanAmount();

        double ltvPct = req.loanAmount() / req.propertyValue() * 100.0;
        String ltvWarning = ltvPct > MAX_RECOMMENDED_LTV_PCT
                ? String.format(Locale.ITALIAN,
                    "Il mutuo richiesto copre il %.1f%% del valore dell'immobile: le banche italiane concedono in genere "
                    + "mutui fondiari fino all'%.0f%% del valore (LTV), oltre questa soglia i tassi applicati sono spesso "
                    + "più alti o è richiesta una garanzia aggiuntiva (es. fideiussione).",
                    ltvPct, MAX_RECOMMENDED_LTV_PCT)
                : null;

        IncomeContext income = resolveIncome(req.monthlyNetIncome());
        double otherDebt = sumOtherActiveDebtPayments();
        double ratioPct = safeRatioPct(monthlyPayment, income.amount());
        double combinedRatioPct = safeRatioPct(monthlyPayment + otherDebt, income.amount());

        double stressRate = req.interestRatePct() + STRESS_TEST_RATE_INCREASE_PCT;
        double stressPayment = calcMonthlyPayment(req.loanAmount(), stressRate, months);
        double stressCombinedRatioPct = safeRatioPct(stressPayment + otherDebt, income.amount());
        String stressWarning = (combinedRatioPct <= MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT
                && stressCombinedRatioPct > MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT)
                ? String.format(Locale.ITALIAN,
                    "Con un rialzo dei tassi di %.1f punti percentuali (rilevante se il mutuo è a tasso variabile) la rata "
                    + "salirebbe a %.0f €/mese e il rapporto rata/reddito complessivo salirebbe al %.1f%%, superando la soglia "
                    + "di sostenibilità del %.0f%%: valuta un tasso fisso o un margine di sicurezza sul reddito prima di impegnarti.",
                    STRESS_TEST_RATE_INCREASE_PCT, round(stressPayment), stressCombinedRatioPct, MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT)
                : null;

        double ancillaryCosts = req.propertyValue() * ANCILLARY_COSTS_PCT_OF_PROPERTY / 100.0;

        return new MortgageSimulationDto(
                round(monthlyPayment), round(totalPaid), round(totalInterest),
                round(ltvPct), ltvWarning,
                round(income.amount()), income.estimated(),
                round(otherDebt), round(ratioPct), round(combinedRatioPct),
                affordabilityLabel(combinedRatioPct), affordabilityWarning(combinedRatioPct, otherDebt),
                round(stressRate), round(stressPayment), round(stressCombinedRatioPct), stressWarning,
                round(ancillaryCosts),
                buildSchedule(req.loanAmount(), req.interestRatePct(), months, monthlyPayment));
    }

    public LoanSimulationDto simulateLoan(LoanRequest req) {
        double monthlyPayment = calcMonthlyPayment(req.loanAmount(), req.interestRatePct(), req.months());
        double totalPaid = monthlyPayment * req.months();
        double totalInterest = totalPaid - req.loanAmount();

        IncomeContext income = resolveIncome(req.monthlyNetIncome());
        double otherDebt = sumOtherActiveDebtPayments();
        double ratioPct = safeRatioPct(monthlyPayment, income.amount());
        double combinedRatioPct = safeRatioPct(monthlyPayment + otherDebt, income.amount());

        return new LoanSimulationDto(
                round(monthlyPayment), round(totalPaid), round(totalInterest),
                round(income.amount()), income.estimated(),
                round(otherDebt), round(ratioPct), round(combinedRatioPct),
                affordabilityLabel(combinedRatioPct), affordabilityWarning(combinedRatioPct, otherDebt),
                buildSchedule(req.loanAmount(), req.interestRatePct(), req.months(), monthlyPayment));
    }

    /** Reddito netto mensile stimato dal budget, usato per precompilare il calcolatore quando l'utente non ne dichiara uno manualmente. */
    public IncomeEstimateDto getIncomeEstimate() {
        BudgetDto budget = budgetService.computeNextMonthBudget();
        boolean hasData = budget.hasEnoughData() && budget.estimatedIncome() != null && budget.estimatedIncome() > 0;
        return new IncomeEstimateDto(hasData ? budget.estimatedIncome() : null, hasData);
    }

    // ─────────────────────────────────── Calcolo rata / ammortamento ──────────

    /** Rata costante con piano di ammortamento alla francese: R = C * i / (1 - (1+i)^-n), con i tasso mensile e n numero di rate. */
    private double calcMonthlyPayment(double principal, double annualRatePct, int months) {
        double monthlyRate = annualRatePct / 100.0 / 12.0;
        if (monthlyRate == 0) return principal / months;
        return principal * monthlyRate / (1 - Math.pow(1 + monthlyRate, -months));
    }

    private List<AmortizationYearDto> buildSchedule(double principal, double annualRatePct, int months, double monthlyPayment) {
        double monthlyRate = annualRatePct / 100.0 / 12.0;
        double balance = principal;
        List<AmortizationYearDto> schedule = new ArrayList<>();

        int year = 1;
        double yearPrincipal = 0;
        double yearInterest = 0;
        for (int month = 1; month <= months; month++) {
            double interest = balance * monthlyRate;
            double principalPaid = Math.min(monthlyPayment - interest, balance);
            balance = Math.max(0, balance - principalPaid);
            yearPrincipal += principalPaid;
            yearInterest += interest;

            boolean yearBoundary = month % 12 == 0;
            boolean lastMonth = month == months;
            if (yearBoundary || lastMonth) {
                schedule.add(new AmortizationYearDto(year, round(yearPrincipal), round(yearInterest), round(balance)));
                year++;
                yearPrincipal = 0;
                yearInterest = 0;
            }
        }
        return schedule;
    }

    // ─────────────────────────────────── Sostenibilità (rata/reddito) ─────────

    private record IncomeContext(double amount, boolean estimated) {}

    private IncomeContext resolveIncome(Double declared) {
        if (declared != null && declared > 0) return new IncomeContext(declared, false);

        BudgetDto budget = budgetService.computeNextMonthBudget();
        if (budget.hasEnoughData() && budget.estimatedIncome() != null && budget.estimatedIncome() > 0) {
            return new IncomeContext(budget.estimatedIncome(), true);
        }
        throw new FinaiException(
                "Reddito netto mensile non disponibile: inseriscilo manualmente oppure importa un estratto conto "
                + "nella sezione Finanza Personale per stimarlo automaticamente.", 422);
    }

    /** Somma le rate mensili di altri debiti/finanziamenti già tra le spese fisse (tasso di interesse dichiarato): la vera base per il rapporto rata/reddito è la rata complessiva, non solo quella del nuovo mutuo/finanziamento. */
    private double sumOtherActiveDebtPayments() {
        return fixedExpenseRepo.findByActiveTrue().stream()
                .filter(e -> e.getInterestRatePct() != null)
                .map(FixedExpense::getAmount)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
    }

    private double safeRatioPct(double payment, double income) {
        return income > 0 ? payment / income * 100.0 : 0.0;
    }

    private String affordabilityLabel(double combinedRatioPct) {
        if (combinedRatioPct <= COMFORTABLE_PAYMENT_TO_INCOME_PCT) return "Sostenibile";
        if (combinedRatioPct <= MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT) return "Al limite";
        return "Rischioso";
    }

    private String affordabilityWarning(double combinedRatioPct, double otherDebt) {
        if (combinedRatioPct <= MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT) return null;
        String debtNote = otherDebt > 0
                ? String.format(Locale.ITALIAN, ", di cui %.0f €/mese di altri debiti già in essere", otherDebt)
                : "";
        return String.format(Locale.ITALIAN,
                "Il rapporto rata/reddito complessivo è %.1f%%%s: supera la soglia del %.0f%% generalmente considerata "
                + "sostenibile dalle banche in fase di istruttoria. Valuta un importo inferiore, una durata più lunga "
                + "(rata più bassa ma interessi totali più alti) o un reddito/garante aggiuntivo.",
                combinedRatioPct, debtNote, MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
