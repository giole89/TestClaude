package com.finai.repository;

import com.finai.domain.entity.SimPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SimPositionRepository extends JpaRepository<SimPosition, String> {

    Optional<SimPosition> findByTicker(String ticker);

    List<SimPosition> findAllByOrderByCreatedAtDesc();
}
