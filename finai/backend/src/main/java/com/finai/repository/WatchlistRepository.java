package com.finai.repository;

import com.finai.domain.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, String> {

    List<WatchlistItem> findAllByOrderByCreatedAtDesc();

    Optional<WatchlistItem> findByTickerIgnoreCase(String ticker);

    boolean existsByTickerIgnoreCase(String ticker);
}
