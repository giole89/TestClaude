package com.finai.controller;

import com.finai.dto.finance.*;
import com.finai.dto.finance.mortgage.*;
import com.finai.exception.FinaiException;
import com.finai.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Modulo di finanza personale: import estratti conto (PDF/Excel), spese
 * fisse, budget previsionale del mese successivo e questionario per il
 * consiglio di investimento.
 */
@RestController
@RequestMapping("/api/finance")
@Tag(name = "Finance", description = "Finanza personale: budget, spese fisse, estratti conto, profilo investitore")
public class FinanceController {

    private final FinanceService service;

    public FinanceController(FinanceService service) {
        this.service = service;
    }

    // ─────────────────────────────────── Estratti conto ──────────────────────

    @PostMapping(value = "/statements/upload", consumes = "multipart/form-data")
    @Operation(summary = "Importa un estratto conto (PDF, XLSX o XLS) e categorizza i movimenti")
    public ResponseEntity<StatementUploadResultDto> uploadStatement(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new FinaiException("File vuoto", 422);
        try {
            return ResponseEntity.ok(service.importStatement(file.getOriginalFilename(), file.getInputStream()));
        } catch (IOException e) {
            throw new FinaiException("Errore lettura file: " + e.getMessage(), 422);
        }
    }

    @GetMapping("/transactions")
    @Operation(summary = "Elenco movimenti importati, più recenti primi")
    public ResponseEntity<List<TransactionDto>> transactions() {
        return ResponseEntity.ok(service.getTransactions());
    }

    @GetMapping("/transactions/categories")
    @Operation(summary = "Categorie note per il menu a tendina di correzione manuale di un movimento")
    public ResponseEntity<TransactionCategoriesDto> transactionCategories() {
        return ResponseEntity.ok(service.getTransactionCategories());
    }

    @PutMapping("/transactions/{id}")
    @Operation(summary = "Corregge manualmente categoria/tipo di un movimento (es. entrata classificata come uscita, o categoria 'Altro')")
    public ResponseEntity<TransactionDto> updateTransaction(@PathVariable String id, @Valid @RequestBody TransactionUpdateRequest req) {
        return ResponseEntity.ok(service.updateTransaction(id, req));
    }

    @DeleteMapping("/transactions/{id}")
    @Operation(summary = "Elimina un movimento importato (es. duplicato o errore di parsing)")
    public ResponseEntity<Void> deleteTransaction(@PathVariable String id) {
        service.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/transactions")
    @Operation(summary = "Elimina più movimenti importati in un colpo solo (selezione multipla)")
    public ResponseEntity<DeleteCountDto> deleteTransactions(@RequestBody List<String> ids) {
        return ResponseEntity.ok(new DeleteCountDto(service.deleteTransactions(ids)));
    }

    @DeleteMapping("/transactions/all")
    @Operation(summary = "Elimina tutti i movimenti importati")
    public ResponseEntity<DeleteCountDto> deleteAllTransactions() {
        return ResponseEntity.ok(new DeleteCountDto(service.deleteAllTransactions()));
    }

    // ─────────────────────────────────── Spese fisse ──────────────────────────

    @GetMapping("/fixed-expenses")
    @Operation(summary = "Elenco spese fisse mensili")
    public ResponseEntity<List<FixedExpenseDto>> fixedExpenses() {
        return ResponseEntity.ok(service.getFixedExpenses());
    }

    @PostMapping("/fixed-expenses")
    @Operation(summary = "Crea una nuova spesa fissa mensile")
    public ResponseEntity<FixedExpenseDto> createFixedExpense(@Valid @RequestBody FixedExpenseRequest req) {
        return ResponseEntity.ok(service.createFixedExpense(req));
    }

    @PutMapping("/fixed-expenses/{id}")
    @Operation(summary = "Aggiorna una spesa fissa esistente")
    public ResponseEntity<FixedExpenseDto> updateFixedExpense(@PathVariable String id, @Valid @RequestBody FixedExpenseRequest req) {
        return ResponseEntity.ok(service.updateFixedExpense(id, req));
    }

    @DeleteMapping("/fixed-expenses/{id}")
    @Operation(summary = "Elimina una spesa fissa")
    public ResponseEntity<Void> deleteFixedExpense(@PathVariable String id) {
        service.deleteFixedExpense(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────── Budget ────────────────────────────────

    @GetMapping("/budget/next-month")
    @Operation(summary = "Budget previsionale per il mese successivo: entrate, costi fissi/variabili, risparmio investibile")
    public ResponseEntity<BudgetDto> nextMonthBudget() {
        return ResponseEntity.ok(service.getNextMonthBudget());
    }

    @GetMapping("/expenses/current-month")
    @Operation(summary = "Spese variabili effettivamente sostenute nel mese corrente, per categoria (per il grafico a torta)")
    public ResponseEntity<MonthlyExpensesDto> currentMonthExpenses() {
        return ResponseEntity.ok(service.getCurrentMonthExpenses());
    }

    @GetMapping("/insights")
    @Operation(summary = "Analizza i movimenti reali importati e produce suggerimenti di risparmio concreti")
    public ResponseEntity<SpendingInsightsDto> insights() {
        return ResponseEntity.ok(service.getSpendingInsights());
    }

    // ─────────────────────────────────── Questionario / advisor ───────────────

    @GetMapping("/questionnaire")
    @Operation(summary = "Stato del questionario investitore")
    public ResponseEntity<InvestorProfileDto> questionnaire() {
        return ResponseEntity.ok(service.getInvestorProfile());
    }

    @PostMapping("/questionnaire")
    @Operation(summary = "Invia le risposte al questionario e ottieni il consiglio di investimento")
    public ResponseEntity<RecommendationDto> submitQuestionnaire(@Valid @RequestBody QuestionnaireRequest req) {
        return ResponseEntity.ok(service.submitQuestionnaire(req));
    }

    @GetMapping("/recommendation")
    @Operation(summary = "Consiglio di investimento basato sull'ultimo questionario completato")
    public ResponseEntity<RecommendationDto> recommendation() {
        return ResponseEntity.ok(service.getRecommendation());
    }

    // ─────────────────────────────────── Mutuo / Finanziamenti ────────────────

    @GetMapping("/mortgage/income-estimate")
    @Operation(summary = "Reddito netto mensile stimato dal budget, per precompilare il calcolatore mutuo/finanziamento")
    public ResponseEntity<IncomeEstimateDto> mortgageIncomeEstimate() {
        return ResponseEntity.ok(service.getIncomeEstimate());
    }

    @PostMapping("/mortgage/simulate")
    @Operation(summary = "Simula un mutuo: rata, LTV, rapporto rata/reddito complessivo e stress test tassi")
    public ResponseEntity<MortgageSimulationDto> simulateMortgage(@Valid @RequestBody MortgageRequest req) {
        return ResponseEntity.ok(service.simulateMortgage(req));
    }

    @PostMapping("/loan/simulate")
    @Operation(summary = "Simula un finanziamento/prestito personale: rata, costo totale e rapporto rata/reddito complessivo")
    public ResponseEntity<LoanSimulationDto> simulateLoan(@Valid @RequestBody LoanRequest req) {
        return ResponseEntity.ok(service.simulateLoan(req));
    }
}
