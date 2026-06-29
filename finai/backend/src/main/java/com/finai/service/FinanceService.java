package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.domain.entity.FixedExpense;
import com.finai.domain.entity.InvestorProfile;
import com.finai.dto.finance.*;
import com.finai.exception.FinaiException;
import com.finai.repository.BankTransactionRepository;
import com.finai.repository.FixedExpenseRepository;
import com.finai.repository.InvestorProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic del modulo di finanza personale: import estratti conto,
 * gestione spese fisse, budget previsionale e questionario investitore.
 */
@Service
@Transactional(readOnly = true)
public class FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceService.class);
    private static final String PROFILE_ID = "default";

    private final BankTransactionRepository transactionRepo;
    private final FixedExpenseRepository    fixedExpenseRepo;
    private final InvestorProfileRepository profileRepo;
    private final StatementParserService    parser;
    private final TransactionCategorizer    categorizer;
    private final BudgetService             budgetService;
    private final InvestmentAdvisorService  advisorService;
    private final PortfolioBuilderService   portfolioBuilder;

    public FinanceService(BankTransactionRepository transactionRepo,
                          FixedExpenseRepository fixedExpenseRepo,
                          InvestorProfileRepository profileRepo,
                          StatementParserService parser,
                          TransactionCategorizer categorizer,
                          BudgetService budgetService,
                          InvestmentAdvisorService advisorService,
                          PortfolioBuilderService portfolioBuilder) {
        this.transactionRepo = transactionRepo;
        this.fixedExpenseRepo = fixedExpenseRepo;
        this.profileRepo = profileRepo;
        this.parser = parser;
        this.categorizer = categorizer;
        this.budgetService = budgetService;
        this.advisorService = advisorService;
        this.portfolioBuilder = portfolioBuilder;
    }

    // ─────────────────────────────────── Estratti conto ──────────────────────

    @Transactional
    public StatementUploadResultDto importStatement(String filename, InputStream input) {
        List<RawTransaction> raw = parser.parse(filename, input);
        if (raw.isEmpty()) {
            throw new FinaiException("Nessun movimento riconosciuto nel file. Verifica il formato dell'estratto conto.", 422);
        }

        int skipped = 0;
        int duplicates = 0;
        List<BankTransaction> saved = new java.util.ArrayList<>();
        for (RawTransaction rt : raw) {
            if (rt.amount() == null || rt.date() == null) { skipped++; continue; }

            // Stesso estratto conto importato più di una volta (o file diversi con movimenti
            // coincidenti): non duplicare il movimento, segnala soltanto quanti ne sono stati trovati.
            if (transactionRepo.existsByTxDateAndDescriptionAndAmount(rt.date(), rt.description(), rt.amount())) {
                duplicates++;
                continue;
            }

            TransactionCategorizer.Classification classification = (rt.sourceCategory() != null && !rt.sourceCategory().isBlank())
                    ? categorizer.classifyWithSourceCategory(rt.sourceCategory(), rt.amount())
                    : categorizer.classify(rt.description(), rt.amount());

            BankTransaction tx = new BankTransaction();
            tx.setId(UUID.randomUUID().toString());
            tx.setTxDate(rt.date());
            tx.setDescription(rt.description());
            tx.setAmount(rt.amount());
            tx.setCategory(classification.category());
            tx.setType(classification.type());
            tx.setSourceFile(filename);
            saved.add(transactionRepo.save(tx));
        }

        log.info("Import estratto conto {}: {} movimenti importati, {} scartati, {} duplicati ignorati",
                filename, saved.size(), skipped, duplicates);
        List<TransactionDto> dtos = saved.stream().map(TransactionDto::from).toList();
        return new StatementUploadResultDto(saved.size(), skipped, duplicates, dtos);
    }

    public List<TransactionDto> getTransactions() {
        return transactionRepo.findAllByOrderByTxDateDesc().stream().map(TransactionDto::from).toList();
    }

    /**
     * Categorie note per il menu a tendina usato nella correzione manuale dei movimenti: unisce
     * le categorie statiche del categorizzatore con quelle effettivamente in uso sui movimenti
     * importati (es. categorie fornite direttamente dalla banca nell'estratto conto), così il
     * menu riflette anche etichette non previste a priori.
     */
    public TransactionCategoriesDto getTransactionCategories() {
        return new TransactionCategoriesDto(
                mergeCategories(TransactionCategorizer.INCOME),
                mergeCategories(TransactionCategorizer.VARIABLE));
    }

    private List<String> mergeCategories(String type) {
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>(categorizer.knownCategories(type));
        names.addAll(transactionRepo.findDistinctCategoriesByType(type));
        return List.copyOf(names);
    }

    @Transactional
    public void deleteTransaction(String id) {
        if (!transactionRepo.existsById(id)) throw new FinaiException("Movimento non trovato", 404);
        transactionRepo.deleteById(id);
    }

    /** Corregge manualmente categoria/tipo di un movimento non riconosciuto bene dalla categorizzazione automatica. */
    @Transactional
    public TransactionDto updateTransaction(String id, TransactionUpdateRequest req) {
        BankTransaction tx = transactionRepo.findById(id)
                .orElseThrow(() -> new FinaiException("Movimento non trovato", 404));
        tx.setCategory(req.category());
        if (!req.type().equals(tx.getType())) {
            tx.setAmount(req.type().equals(TransactionCategorizer.INCOME) ? tx.getAmount().abs() : tx.getAmount().abs().negate());
        }
        tx.setType(req.type());
        return TransactionDto.from(transactionRepo.save(tx));
    }

    /** Elimina più movimenti in un colpo solo (es. selezione multipla dalla lista importati). */
    @Transactional
    public int deleteTransactions(List<String> ids) {
        if (ids == null || ids.isEmpty()) return 0;
        List<String> existing = ids.stream().distinct().filter(transactionRepo::existsById).toList();
        transactionRepo.deleteAllById(existing);
        return existing.size();
    }

    /** Elimina tutti i movimenti importati (reset rapido prima di un nuovo import). */
    @Transactional
    public int deleteAllTransactions() {
        long count = transactionRepo.count();
        transactionRepo.deleteAll();
        return (int) count;
    }

    // ─────────────────────────────────── Spese fisse ──────────────────────────

    public List<FixedExpenseDto> getFixedExpenses() {
        return fixedExpenseRepo.findAllByOrderByCreatedAtDesc().stream().map(FixedExpenseDto::from).toList();
    }

    @Transactional
    public FixedExpenseDto createFixedExpense(FixedExpenseRequest req) {
        FixedExpense e = new FixedExpense();
        e.setId(UUID.randomUUID().toString());
        applyRequest(e, req);
        return FixedExpenseDto.from(fixedExpenseRepo.save(e));
    }

    @Transactional
    public FixedExpenseDto updateFixedExpense(String id, FixedExpenseRequest req) {
        FixedExpense e = fixedExpenseRepo.findById(id)
                .orElseThrow(() -> new FinaiException("Spesa fissa non trovata", 404));
        applyRequest(e, req);
        e.setUpdatedAt(Instant.now());
        return FixedExpenseDto.from(fixedExpenseRepo.save(e));
    }

    @Transactional
    public void deleteFixedExpense(String id) {
        if (!fixedExpenseRepo.existsById(id)) throw new FinaiException("Spesa fissa non trovata", 404);
        fixedExpenseRepo.deleteById(id);
    }

    private void applyRequest(FixedExpense e, FixedExpenseRequest req) {
        e.setName(req.name());
        e.setCategory(req.category());
        e.setAmount(java.math.BigDecimal.valueOf(req.amount()));
        e.setActive(req.active() == null || req.active());
    }

    // ─────────────────────────────────── Budget ────────────────────────────────

    public BudgetDto getNextMonthBudget() {
        return budgetService.computeNextMonthBudget();
    }

    public MonthlyExpensesDto getCurrentMonthExpenses() {
        return budgetService.computeCurrentMonthExpenses();
    }

    // ─────────────────────────────────── Questionario / advisor ───────────────

    public InvestorProfileDto getInvestorProfile() {
        return InvestorProfileDto.from(loadProfile());
    }

    @Transactional
    public RecommendationDto submitQuestionnaire(QuestionnaireRequest req) {
        InvestorProfile profile = loadProfile();
        profile.setGoal(req.goal().toUpperCase());
        profile.setGoalNote(req.goalNote());
        profile.setHorizon(req.horizon().toUpperCase());
        profile.setUpdatedAt(Instant.now());
        profileRepo.save(profile);
        return withMarketPortfolio(advisorService.recommend(profile));
    }

    public RecommendationDto getRecommendation() {
        InvestorProfile profile = loadProfile();
        if (profile.getGoal() == null || profile.getHorizon() == null) {
            throw new FinaiException("Questionario non ancora completato", 404);
        }
        return withMarketPortfolio(advisorService.recommend(profile));
    }

    private RecommendationDto withMarketPortfolio(RecommendationDto base) {
        PortfolioBuilderService.Result result = portfolioBuilder.build(base.allocation(), base.goal());
        return new RecommendationDto(base.profileLabel(), base.allocation(), base.summary(), base.suggestedInstruments(),
                base.goal(), base.horizon(), result.snapshot(), result.portfolio());
    }

    private InvestorProfile loadProfile() {
        return profileRepo.findById(PROFILE_ID).orElseGet(() -> {
            InvestorProfile p = new InvestorProfile();
            p.setId(PROFILE_ID);
            return profileRepo.save(p);
        });
    }
}
