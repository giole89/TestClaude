package com.finai.service;

import com.finai.domain.entity.BankTransaction;
import com.finai.dto.finance.RawTransaction;
import com.finai.dto.finance.StatementUploadResultDto;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    private InvestmentAdvisorService advisorService;
    @Mock
    private PortfolioBuilderService portfolioBuilder;

    private FinanceService service;

    @BeforeEach
    void setUp() {
        service = new FinanceService(transactionRepo, fixedExpenseRepo, profileRepo, parser,
                new TransactionCategorizer(), budgetService, advisorService, portfolioBuilder);
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
}
