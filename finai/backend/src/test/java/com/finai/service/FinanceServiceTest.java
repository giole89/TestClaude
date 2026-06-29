package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.dto.finance.RawTransaction;
import com.finai.dto.finance.StatementUploadResultDto;
import com.finai.dto.finance.TransactionDto;
import com.finai.dto.finance.TransactionUpdateRequest;
import com.finai.exception.FinaiException;
import com.finai.repository.BankTransactionRepository;
import com.finai.repository.FixedExpenseRepository;
import com.finai.repository.InvestorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitari per {@link FinanceService}: gestione duplicati ed eliminazione massiva dei movimenti.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinanceService")
class FinanceServiceTest {

    @Mock
    private BankTransactionRepository transactionRepo;
    @Mock
    private FixedExpenseRepository fixedExpenseRepo;
    @Mock
    private InvestorProfileRepository profileRepo;
    @Mock
    private StatementParserService parser;
    @Mock
    private BudgetService budgetService;
    @Mock
    private SpendingInsightsService insightsService;
    @Mock
    private InvestmentAdvisorService advisorService;
    @Mock
    private PortfolioBuilderService portfolioBuilder;

    private FinanceService service;

    @BeforeEach
    void setUp() {
        service = new FinanceService(transactionRepo, fixedExpenseRepo, profileRepo, parser,
                new TransactionCategorizer(), budgetService, insightsService, advisorService, portfolioBuilder);
        lenient().when(transactionRepo.save(any(BankTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("non importa di nuovo un movimento già presente (stessa data/descrizione/importo) e lo conta come duplicato")
    void skipsDuplicateTransactionsOnImport() {
        RawTransaction rt = new RawTransaction(LocalDate.of(2024, 3, 1), "Stipendio", new BigDecimal("1500.00"));
        when(parser.parse(eq("estratto.pdf"), any())).thenReturn(List.of(rt));
        when(transactionRepo.existsByTxDateAndDescriptionAndAmount(rt.date(), rt.description(), rt.amount())).thenReturn(true);

        StatementUploadResultDto result = service.importStatement("estratto.pdf", new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isZero();
        assertThat(result.duplicates()).isEqualTo(1);
        verify(transactionRepo, never()).save(any());
    }

    @Test
    @DisplayName("importa normalmente un movimento non duplicato")
    void importsNonDuplicateTransactions() {
        RawTransaction rt = new RawTransaction(LocalDate.of(2024, 3, 1), "Stipendio", new BigDecimal("1500.00"));
        when(parser.parse(eq("estratto.pdf"), any())).thenReturn(List.of(rt));
        when(transactionRepo.existsByTxDateAndDescriptionAndAmount(rt.date(), rt.description(), rt.amount())).thenReturn(false);

        StatementUploadResultDto result = service.importStatement("estratto.pdf", new ByteArrayInputStream(new byte[0]));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.duplicates()).isZero();
        verify(transactionRepo).save(any(BankTransaction.class));
    }

    @Test
    @DisplayName("usa la categoria fornita dall'estratto conto (es. colonna CATEGORIA della banca) invece di indovinarla dalla descrizione")
    void prefersSourceCategoryOverKeywordGuessing() {
        RawTransaction rt = new RawTransaction(LocalDate.of(2026, 6, 21), "Ipertosano Cesano Boscone",
                new BigDecimal("-33.64"), "Generi alimentari e supermercato");
        when(parser.parse(eq("estratto.pdf"), any())).thenReturn(List.of(rt));
        when(transactionRepo.existsByTxDateAndDescriptionAndAmount(rt.date(), rt.description(), rt.amount())).thenReturn(false);

        service.importStatement("estratto.pdf", new ByteArrayInputStream(new byte[0]));

        verify(transactionRepo).save(argThat(tx ->
                tx.getCategory().equals("Generi alimentari e supermercato") && tx.getType().equals(TransactionCategorizer.VARIABLE)));
    }

    @Test
    @DisplayName("elimina più movimenti selezionati in un colpo solo, ignorando id inesistenti")
    void deletesSelectedTransactions() {
        when(transactionRepo.existsById("a")).thenReturn(true);
        when(transactionRepo.existsById("b")).thenReturn(true);
        when(transactionRepo.existsById("missing")).thenReturn(false);

        int deleted = service.deleteTransactions(List.of("a", "b", "missing"));

        assertThat(deleted).isEqualTo(2);
        verify(transactionRepo).deleteAllById(List.of("a", "b"));
    }

    @Test
    @DisplayName("elimina tutti i movimenti importati")
    void deletesAllTransactions() {
        when(transactionRepo.count()).thenReturn(5L);

        int deleted = service.deleteAllTransactions();

        assertThat(deleted).isEqualTo(5);
        verify(transactionRepo).deleteAll();
    }

    @Test
    @DisplayName("corregge categoria e tipo di un movimento, riallineando il segno dell'importo al nuovo tipo")
    void updatesTransactionCategoryAndType() {
        BankTransaction tx = new BankTransaction();
        tx.setId("a");
        tx.setTxDate(LocalDate.of(2024, 3, 1));
        tx.setDescription("Bonifico generico");
        tx.setAmount(new BigDecimal("-200.00"));
        tx.setCategory("Altro");
        tx.setType(TransactionCategorizer.VARIABLE);
        when(transactionRepo.findById("a")).thenReturn(Optional.of(tx));

        TransactionDto result = service.updateTransaction("a", new TransactionUpdateRequest("Stipendio", TransactionCategorizer.INCOME));

        assertThat(result.category()).isEqualTo("Stipendio");
        assertThat(result.type()).isEqualTo(TransactionCategorizer.INCOME);
        assertThat(result.amount()).isEqualTo(200.00);
    }

    @Test
    @DisplayName("il menu delle categorie include anche quelle realmente in uso sui movimenti, non solo quelle statiche note")
    void transactionCategoriesIncludeActuallyUsedOnes() {
        when(transactionRepo.findDistinctCategoriesByType(TransactionCategorizer.VARIABLE))
                .thenReturn(List.of("Generi alimentari e supermercato", "Altre uscite"));
        when(transactionRepo.findDistinctCategoriesByType(TransactionCategorizer.INCOME))
                .thenReturn(List.of("Stipendi e pensioni"));

        var categories = service.getTransactionCategories();

        assertThat(categories.expense()).contains("Alimentari", "Generi alimentari e supermercato", "Altre uscite");
        assertThat(categories.income()).contains("Stipendio", "Stipendi e pensioni");
    }

    @Test
    @DisplayName("segnala errore se si tenta di correggere un movimento inesistente")
    void failsToUpdateMissingTransaction() {
        when(transactionRepo.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTransaction("missing", new TransactionUpdateRequest("Altro", TransactionCategorizer.VARIABLE)))
                .isInstanceOf(FinaiException.class);
    }
}
