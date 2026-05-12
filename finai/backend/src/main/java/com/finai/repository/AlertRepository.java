package com.finai.repository;

import com.finai.domain.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository per gli alert sui prezzi. */
@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {

    /** Alert attivi: firedAt è null (non ancora scattati). */
    List<Alert> findByFiredAtIsNullOrderByCreatedAtDesc();

    /** Alert in history: firedAt è valorizzato. */
    List<Alert> findByFiredAtIsNotNullOrderByFiredAtDesc();
}
