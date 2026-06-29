package com.finai.repository;

import com.finai.domain.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, String> {

    List<BankTransaction> findAllByOrderByTxDateDesc();

    List<BankTransaction> findByTxDateGreaterThanEqualOrderByTxDateDesc(LocalDate from);

    List<BankTransaction> findByTxDateBetween(LocalDate from, LocalDate to);

    boolean existsByTxDateAndDescriptionAndAmount(LocalDate txDate, String description, BigDecimal amount);

    @Query("select distinct t.category from BankTransaction t where t.type = :type order by t.category")
    List<String> findDistinctCategoriesByType(@Param("type") String type);
}
