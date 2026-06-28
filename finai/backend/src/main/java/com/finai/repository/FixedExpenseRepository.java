package com.finai.repository;

import com.finai.domain.entity.FixedExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FixedExpenseRepository extends JpaRepository<FixedExpense, String> {

    List<FixedExpense> findAllByOrderByCreatedAtDesc();

    List<FixedExpense> findByActiveTrue();
}
