package com.finai.service;

import com.finai.domain.entity.FixedExpense;
import com.finai.domain.entity.InvestorProfile;
import com.finai.dto.finance.BudgetDto;
import com.finai.dto.finance.mortgage.*;
import com.finai.exception.FinaiException;
import com.finai.repository.FixedExpenseRepository;
import com.finai.repository.InvestorProfileRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Calcolatore di mutui per l'acquisto di una casa: rata con piano di ammortamento alla francese
 * (quota capitale crescente, rata costante — lo standard dei mutui italiani), più gli indicatori
 * che una banca valuterebbe in fase di istruttoria e il quadro completo del costo dell'operazione:
 * <ul>
 *   <li>LTV (loan-to-value): quota del valore dell'immobile coperta dal mutuo richiesto;</li>
 *   <li>rapporto rata/reddito, calcolato non solo sulla nuova rata ma sommandola alle rate di
 *       eventuali altri debiti già tra le spese fisse dell'utente;</li>
 *   <li>stress test: cosa succede alla sostenibilità se il tasso salisse;</li>
 *   <li>tutto ciò che <strong>non</strong> è coperto dal mutuo: capitale proprio (differenza tra
 *       prezzo e mutuo) più le spese accessorie (notaio, istruttoria, perizia, agenzia, imposta di
 *       registro/IVA), dichiarate dall'utente o stimate;</li>
 *   <li>a quali fonti attingere per coprire questo fabbisogno: liquidità disponibile, eventuale
 *       anticipazione del fondo pensione complementare (ammessa solo dopo 8 anni di iscrizione e
 *       solo per la prima casa), risparmio mensile residuo.</li>
 * </ul>
 */
@Service
public class MortgageService {

    private static final String PROFILE_ID = "default";

    /** LTV oltre il quale un mutuo fondiario italiano tipicamente richiede condizioni più severe o garanzie aggiuntive. */
    private static final double MAX_RECOMMENDED_LTV_PCT = 80.0;
    /** Rapporto rata complessiva/reddito oltre il quale la sostenibilità è considerata a rischio (prassi bancaria, linee guida Banca d'Italia). */
    private static final double MAX_RECOMMENDED_PAYMENT_TO_INCOME_PCT = 35.0;
    /** Soglia sotto la quale il rapporto è considerato pienamente sostenibile senza riserve. */
    private static final double COMFORTABLE_PAYMENT_TO_INCOME_PCT = 30.0;
    /** Rialzo di tasso simulato nello stress test (rilevante per mutui a tasso variabile). */
    private static final double STRESS_TEST_RATE_INCREASE_PCT = 2.0;

    private static final double NOTARY_DEFAULT_PCT_OF_PROPERTY = 2.0;
    private static final double ORIGINATION_DEFAULT_PCT_OF_LOAN = 0.5;
    private static final double APPRAISAL_DEFAULT_FLAT = 300.0;
    private static final double AGENCY_DEFAULT_PCT_OF_PROPERTY = 3.0;
    private static final double AGENCY_IVA_MULTIPLIER = 1.22;
    private static final double MIN_REGISTRATION_TAX = 1000.0;
    private static final double BUILDER_FIXED_TAXES = 600.0;

    /** Anni minimi di iscrizione al fondo pensione richiesti per l'anticipazione finalizzata all'acquisto della prima casa (D.Lgs. 252/2005, art. 11 comma 7). */
    private static final int PENSION_FUND_MIN_YEARS_FOR_HOME = 8;
    private static final double PENSION_FUND_MAX_ANTICIPATION_PCT = 75.0;

    /** Anni di possesso sotto i quali la plusvalenza da vendita immobiliare è tassabile, salvo esenzione prima casa (art. 67 TUIR). */
    private static final int CAPITAL_GAINS_EXEMPT_HOLDING_YEARS = 5;
    private static final double CAPITAL_GAINS_SUBSTITUTE_TAX_PCT = 26.0;
    /** Tempo medio indicativo per concludere la vendita di un immobile in Italia, usato solo per un confronto informativo sui tempi. */
    private static final int TYPICAL_SALE_TIMEFRAME_MONTHS = 6;

    private static final Set<String> FIRST_HOME_TYPES = Set.of("PRIMA_CASA_PRIVATO", "PRIMA_CASA_COSTRUTTORE");
    private static final Set<String> VALID_PURCHASE_TYPES = Set.of(
            "PRIMA_CASA_PRIVATO", "PRIMA_CASA_COSTRUTTORE", "SECONDA_CASA_PRIVATO", "SECONDA_CASA_COSTRUTTORE");

    private final FixedExpenseRepository fixedExpenseRepo;
    private final InvestorProfileRepository investorProfileRepo;
    private final BudgetService budgetService;

    public MortgageService(FixedExpenseRepository fixedExpenseRepo, InvestorProfileRepository investorProfileRepo, BudgetService budgetService) {
        this.fixedExpenseRepo = fixedExpenseRepo;
        this.investorProfileRepo = investorProfileRepo;
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

        String purchaseType = resolvePurchaseType(req.purchaseType());
        double propertyValue = req.propertyValue();

        CostLine notary = resolveCost(req.notaryCosts(), propertyValue * NOTARY_DEFAULT_PCT_OF_PROPERTY / 100.0);
        CostLine origination = resolveCost(req.originationFees(), req.loanAmount() * ORIGINATION_DEFAULT_PCT_OF_LOAN / 100.0);
        CostLine appraisal = resolveCost(req.appraisalFees(), APPRAISAL_DEFAULT_FLAT);
        AgencyFeeLine agency = resolveAgencyFee(req.agencyFeePct(), req.agencyFeeAmount(), propertyValue);
        CostLine registrationTax = resolveCost(req.registrationTax(), estimateRegistrationTax(propertyValue, purchaseType));

        double downPayment = Math.max(0, propertyValue - req.loanAmount());
        double totalAncillaryCosts = notary.amount() + origination.amount() + appraisal.amount() + agency.total() + registrationTax.amount();
        double totalOutOfPocketCost = downPayment + totalAncillaryCosts;

        LiquidSavingsContext liquidSavings = resolveLiquidSavings(req.liquidSavings());
        HomeSaleAdviceDto homeSale = homeSaleAdvice(req.homeSale());
        double netSaleProceeds = homeSale != null ? homeSale.netProceeds() : 0.0;
        double totalAvailableCapital = liquidSavings.amount() + Math.max(0, netSaleProceeds);
        double shortfall = Math.max(0, totalOutOfPocketCost - totalAvailableCapital);

        PensionFundAdviceDto pensionFund = pensionFundAdvice(req.pensionFundYears(), req.pensionFundBalance(), purchaseType);
        List<BudgetAdviceDto> budgetAdvice = buildBudgetAdvice(totalOutOfPocketCost, liquidSavings.amount(), homeSale, pensionFund);

        return new MortgageSimulationDto(
                round(monthlyPayment), round(totalPaid), round(totalInterest),
                round(ltvPct), ltvWarning,
                round(income.amount()), income.estimated(),
                round(otherDebt), round(ratioPct), round(combinedRatioPct),
                affordabilityLabel(combinedRatioPct), affordabilityWarning(combinedRatioPct, otherDebt),
                round(stressRate), round(stressPayment), round(stressCombinedRatioPct), stressWarning,
                round(downPayment),
                round(notary.amount()), notary.estimated(),
                round(origination.amount()), origination.estimated(),
                round(appraisal.amount()), appraisal.estimated(),
                round(agency.total()), agency.estimated(), round(agency.base()), round(agency.iva()), agency.mode(),
                round(registrationTax.amount()), registrationTax.estimated(),
                "Stima approssimata sul prezzo dichiarato: per un acquisto da privato l'imposta di registro si calcola in "
                + "realtà sul valore catastale (in genere inferiore al prezzo di mercato), quindi questa stima è spesso "
                + "più alta del dovuto — chiedi il calcolo esatto al notaio prima di impegnarti.",
                round(totalAncillaryCosts), round(totalOutOfPocketCost),
                round(liquidSavings.amount()), liquidSavings.source(),
                homeSale, round(totalAvailableCapital), round(shortfall),
                pensionFund, budgetAdvice,
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

    /** Somma le rate mensili di altri debiti/finanziamenti già tra le spese fisse (tasso di interesse dichiarato): la vera base per il rapporto rata/reddito è la rata complessiva, non solo quella del nuovo mutuo. */
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

    // ─────────────────────────────────── Costo non coperto dal mutuo ──────────

    private record CostLine(double amount, boolean estimated) {}

    private CostLine resolveCost(Double declared, double estimate) {
        return declared != null ? new CostLine(declared, false) : new CostLine(estimate, true);
    }

    private record AgencyFeeLine(double base, double iva, double total, boolean estimated, String mode) {}

    /**
     * La commissione di agenzia in Italia si esprime tipicamente come percentuale + IVA: se l'utente indica una
     * percentuale, l'IVA al 22% viene aggiunta automaticamente al totale. Se indica invece un importo finale in
     * euro (es. da un preventivo), lo si considera già comprensivo di ogni imposta, senza ulteriori aggiunte.
     * Senza alcun dato dichiarato, si stima una commissione standard del 3% + IVA sul valore dell'immobile.
     */
    private AgencyFeeLine resolveAgencyFee(Double declaredPct, Double declaredAmount, double propertyValue) {
        if (declaredPct != null) {
            double base = propertyValue * declaredPct / 100.0;
            double iva = base * (AGENCY_IVA_MULTIPLIER - 1);
            return new AgencyFeeLine(base, iva, base + iva, false, "PERCENTAGE");
        }
        if (declaredAmount != null) {
            return new AgencyFeeLine(declaredAmount, 0, declaredAmount, false, "AMOUNT");
        }
        double base = propertyValue * AGENCY_DEFAULT_PCT_OF_PROPERTY / 100.0;
        double iva = base * (AGENCY_IVA_MULTIPLIER - 1);
        return new AgencyFeeLine(base, iva, base + iva, true, "PERCENTAGE");
    }

    private String resolvePurchaseType(String raw) {
        if (raw == null || raw.isBlank()) return "PRIMA_CASA_PRIVATO";
        String normalized = raw.trim().toUpperCase(Locale.ITALIAN);
        if (!VALID_PURCHASE_TYPES.contains(normalized)) {
            throw new FinaiException("purchaseType non valido: deve essere uno tra " + VALID_PURCHASE_TYPES, 400);
        }
        return normalized;
    }

    private double estimateRegistrationTax(double propertyValue, String purchaseType) {
        return switch (purchaseType) {
            case "PRIMA_CASA_PRIVATO" -> Math.max(propertyValue * 0.02, MIN_REGISTRATION_TAX);
            case "PRIMA_CASA_COSTRUTTORE" -> propertyValue * 0.04 + BUILDER_FIXED_TAXES;
            case "SECONDA_CASA_PRIVATO" -> Math.max(propertyValue * 0.09, MIN_REGISTRATION_TAX);
            case "SECONDA_CASA_COSTRUTTORE" -> propertyValue * 0.10 + BUILDER_FIXED_TAXES;
            default -> Math.max(propertyValue * 0.02, MIN_REGISTRATION_TAX);
        };
    }

    // ─────────────────────────────────── Liquidità e fondo pensione ───────────

    private record LiquidSavingsContext(double amount, String source) {}

    private LiquidSavingsContext resolveLiquidSavings(Double declared) {
        if (declared != null && declared > 0) return new LiquidSavingsContext(declared, "DECLARED");

        InvestorProfile profile = investorProfileRepo.findById(PROFILE_ID).orElse(null);
        if (profile != null && profile.getLiquidSavings() != null && profile.getLiquidSavings().doubleValue() > 0) {
            return new LiquidSavingsContext(profile.getLiquidSavings().doubleValue(), "PROFILE");
        }
        return new LiquidSavingsContext(0.0, "NONE");
    }

    /**
     * Verifica l'idoneità del fondo pensione complementare come fonte per il capitale proprio, secondo
     * il D.Lgs. 252/2005: l'anticipazione fino al 75% del montante per l'acquisto della prima casa (per
     * sé o per i figli) è ammessa solo dopo almeno 8 anni di iscrizione, e mai per la seconda casa
     * (a parte l'eccezione, non pertinente qui, per gravi spese sanitarie ammessa in qualsiasi momento).
     */
    private PensionFundAdviceDto pensionFundAdvice(Integer years, Double balance, String purchaseType) {
        if (years == null) return null;

        boolean isFirstHome = FIRST_HOME_TYPES.contains(purchaseType);
        boolean hasMinYears = years >= PENSION_FUND_MIN_YEARS_FOR_HOME;
        boolean eligible = isFirstHome && hasMinYears;
        int yearsUntilEligible = Math.max(0, PENSION_FUND_MIN_YEARS_FOR_HOME - years);

        Double maxAnticipationPct = eligible ? PENSION_FUND_MAX_ANTICIPATION_PCT : null;
        Double estimatedMax = (eligible && balance != null) ? round(balance * PENSION_FUND_MAX_ANTICIPATION_PCT / 100.0) : null;

        String note;
        if (!isFirstHome) {
            note = "L'anticipazione del fondo pensione per l'acquisto di un immobile è ammessa dalla legge solo per la "
                    + "prima casa (per te o per i tuoi figli), non per la seconda casa: questa fonte non è utilizzabile "
                    + "per l'acquisto che stai valutando, indipendentemente dagli anni di iscrizione.";
        } else if (eligible) {
            note = String.format(Locale.ITALIAN,
                    "Con %d anni di iscrizione hai maturato il requisito minimo di 8 anni: puoi richiedere un'anticipazione "
                    + "fino al %.0f%% del montante accumulato per l'acquisto della prima casa. Ricorda che l'importo anticipato "
                    + "sconta una ritenuta sostitutiva (in genere 15%%, riducibile fino al 9%% dopo 35 anni di partecipazione) "
                    + "e riduce corrispondentemente il capitale disponibile al pensionamento.",
                    years, PENSION_FUND_MAX_ANTICIPATION_PCT)
                    + (balance == null ? " Indica il montante accumulato per stimare l'importo anticipabile." : "");
        } else {
            note = String.format(Locale.ITALIAN,
                    "Con %d anni di iscrizione non hai ancora maturato il requisito minimo di 8 anni previsto dal D.Lgs. "
                    + "252/2005 per l'anticipazione finalizzata all'acquisto della prima casa: mancano ancora %d anni. "
                    + "L'unica anticipazione ammessa in qualsiasi momento riguarda gravi spese sanitarie (terapie o interventi "
                    + "straordinari per te, il coniuge o i figli), non l'acquisto di un immobile: non contare su questa fonte "
                    + "per l'acquisto attuale.",
                    years, yearsUntilEligible);
        }

        return new PensionFundAdviceDto(years, eligible, yearsUntilEligible, maxAnticipationPct, estimatedMax, note);
    }

    // ─────────────────────────────────── Vendita casa esistente ───────────────

    /**
     * Stima il capitale disponibile dalla vendita di una casa esistente: valore di vendita meno spese di
     * agenzia, meno l'eventuale mutuo/finanziamento residuo da estinguere, meno l'eventuale imposta sulla
     * plusvalenza. La plusvalenza (art. 67 TUIR) è tassabile con imposta sostitutiva del 26% solo se
     * l'immobile è posseduto da meno di 5 anni e non è stato abitazione principale per la maggior parte del
     * periodo di possesso; altrimenti è sempre esente.
     */
    private HomeSaleAdviceDto homeSaleAdvice(HomeSaleRequest req) {
        if (req == null) return null;

        double capitalGain = Math.max(0, req.saleValue() - req.purchasePrice());
        boolean mainResidence = Boolean.TRUE.equals(req.mainResidence());
        boolean withinExemptWindow = req.yearsOwned() < CAPITAL_GAINS_EXEMPT_HOLDING_YEARS;
        boolean taxable = capitalGain > 0 && withinExemptWindow && !mainResidence;
        double tax = taxable ? capitalGain * CAPITAL_GAINS_SUBSTITUTE_TAX_PCT / 100.0 : 0.0;

        String capitalGainsNote;
        if (capitalGain <= 0) {
            capitalGainsNote = "Nessuna plusvalenza da tassare: il prezzo di vendita non supera quello di acquisto.";
        } else if (taxable) {
            capitalGainsNote = String.format(Locale.ITALIAN,
                    "La plusvalenza di %.0f € è tassabile (immobile posseduto da meno di %d anni e non abitazione principale "
                    + "per la maggior parte del periodo di possesso): imposta sostitutiva del %.0f%% pari a %.0f € (in alternativa "
                    + "puoi optare in dichiarazione per la tassazione IRPEF ordinaria, spesso meno conveniente).",
                    capitalGain, CAPITAL_GAINS_EXEMPT_HOLDING_YEARS, CAPITAL_GAINS_SUBSTITUTE_TAX_PCT, tax);
        } else {
            capitalGainsNote = String.format(Locale.ITALIAN,
                    "La plusvalenza di %.0f € non è tassabile: %s.", capitalGain,
                    mainResidence
                            ? "l'immobile è stato abitazione principale per la maggior parte del periodo di possesso"
                            : String.format(Locale.ITALIAN, "sono passati almeno %d anni dall'acquisto", CAPITAL_GAINS_EXEMPT_HOLDING_YEARS));
        }

        CostLine saleAgency = resolveCost(req.saleAgencyFees(), req.saleValue() * AGENCY_DEFAULT_PCT_OF_PROPERTY / 100.0 * AGENCY_IVA_MULTIPLIER);
        double residual = req.residualMortgageBalance() != null ? req.residualMortgageBalance() : 0.0;
        double netProceeds = req.saleValue() - saleAgency.amount() - residual - tax;

        String timingNote = null;
        if (req.monthsUntilSale() != null) {
            timingNote = req.monthsUntilSale() < TYPICAL_SALE_TIMEFRAME_MONTHS
                    ? String.format(Locale.ITALIAN,
                        "Prevedi di completare la vendita in %d mesi: il tempo medio per vendere un immobile in Italia è di circa "
                        + "%d mesi. Se il capitale ti serve per la data prevista, valuta un margine di sicurezza, un mutuo ponte, "
                        + "o un compromesso di acquisto condizionato al buon esito della vendita.",
                        req.monthsUntilSale(), TYPICAL_SALE_TIMEFRAME_MONTHS)
                    : String.format(Locale.ITALIAN,
                        "La tempistica prevista (%d mesi) è in linea con il tempo medio di vendita di un immobile in Italia (~%d mesi).",
                        req.monthsUntilSale(), TYPICAL_SALE_TIMEFRAME_MONTHS);
        }

        String summary = netProceeds > 0
                ? String.format(Locale.ITALIAN,
                    "Dalla vendita ricaveresti circa %.0f € netti (dopo spese di agenzia%s%s) da usare come capitale per il nuovo acquisto.",
                    netProceeds, residual > 0 ? ", estinzione del mutuo residuo" : "", taxable ? " e imposta sulla plusvalenza" : "")
                : String.format(Locale.ITALIAN,
                    "Attenzione: dopo spese di agenzia%s%s, la vendita non libererebbe capitale — mancherebbero ancora circa %.0f €.",
                    residual > 0 ? ", estinzione del mutuo residuo" : "", taxable ? " e imposta sulla plusvalenza" : "", Math.abs(netProceeds));

        return new HomeSaleAdviceDto(
                round(capitalGain), taxable, round(tax), capitalGainsNote,
                round(saleAgency.amount()), saleAgency.estimated(),
                round(residual), round(netProceeds),
                req.monthsUntilSale(), timingNote, summary);
    }

    /** Elenco ordinato di fonti a cui attingere per coprire capitale proprio e spese accessorie non finanziate dal mutuo. */
    private List<BudgetAdviceDto> buildBudgetAdvice(double totalOutOfPocketCost, double availableLiquidSavings, HomeSaleAdviceDto homeSale, PensionFundAdviceDto pensionFund) {
        List<BudgetAdviceDto> advice = new ArrayList<>();
        double remaining = totalOutOfPocketCost;

        if (availableLiquidSavings > 0) {
            double used = Math.min(availableLiquidSavings, remaining);
            remaining -= used;
            advice.add(new BudgetAdviceDto("Liquidità disponibile",
                    String.format(Locale.ITALIAN, "Copre %.0f € dei %.0f € necessari con la liquidità dichiarata.", used, totalOutOfPocketCost),
                    round(used)));
        }

        if (homeSale != null) {
            if (homeSale.netProceeds() > 0) {
                double used = Math.min(homeSale.netProceeds(), Math.max(0, remaining));
                remaining -= used;
                advice.add(new BudgetAdviceDto("Vendita immobile esistente",
                        String.format(Locale.ITALIAN, "%s Copre %.0f € del fabbisogno.", homeSale.summary(), used),
                        round(used)));
            } else {
                advice.add(new BudgetAdviceDto("Vendita immobile esistente", homeSale.summary(), 0.0));
            }
        }

        if (remaining > 0 && pensionFund != null && pensionFund.eligibleForHomePurchase() && pensionFund.estimatedMaxAnticipation() != null) {
            double used = Math.min(pensionFund.estimatedMaxAnticipation(), remaining);
            remaining -= used;
            advice.add(new BudgetAdviceDto("Anticipazione fondo pensione",
                    String.format(Locale.ITALIAN, "Sei idoneo all'anticipazione per prima casa: fino a %.0f € stimati disponibili.", used),
                    round(used)));
        }

        if (remaining > 0) {
            BudgetDto budget = budgetService.computeNextMonthBudget();
            double investable = budget.investableAmount() != null ? budget.investableAmount() : 0;
            if (investable > 0) {
                int monthsNeeded = (int) Math.ceil(remaining / investable);
                advice.add(new BudgetAdviceDto("Risparmio mensile residuo",
                        String.format(Locale.ITALIAN,
                                "Con una quota investibile mensile stimata di %.0f €, servirebbero circa %d mesi di risparmio per "
                                + "accantonare i %.0f € ancora mancanti.", investable, monthsNeeded, remaining),
                        null));
            } else {
                advice.add(new BudgetAdviceDto("Risparmio mensile residuo",
                        "Non ci sono ancora dati sufficienti sul budget mensile per stimare un piano di accantonamento: "
                        + "importa un estratto conto nella sezione Finanza Personale per una stima più precisa.",
                        null));
            }
            advice.add(new BudgetAdviceDto("Altre opzioni",
                    "Valuta di richiedere più preventivi per ridurre le spese accessorie (notaio, agenzia), aumentare la quota "
                    + "finanziata dal mutuo se il rapporto rata/reddito resta sostenibile, chiedere un aiuto familiare, o posticipare "
                    + "l'acquisto finché il fabbisogno residuo non sarà coperto.",
                    null));
        }

        if (advice.isEmpty()) {
            advice.add(new BudgetAdviceDto("Copertura completa",
                    "La liquidità dichiarata copre l'intero capitale proprio e le spese accessorie stimate: nessuna fonte aggiuntiva è necessaria.",
                    0.0));
        }

        return advice;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
