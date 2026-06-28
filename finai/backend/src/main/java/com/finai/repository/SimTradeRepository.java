package com.finai.repository;

import com.finai.domain.entity.SimTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimTradeRepository extends JpaRepository<SimTrade, String> {

    List<SimTrade> findAllByOrderByExecutedAtDesc();
}
