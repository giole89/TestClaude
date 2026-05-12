package com.finai.repository;

import com.finai.domain.entity.IpoWatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Repository per la watchlist IPO personale. */
@Repository
public interface IpoWatchlistRepository extends JpaRepository<IpoWatchlistItem, String> {

    /** Restituisce tutti gli elementi ordinati per data prevista (più vicini prima). */
    List<IpoWatchlistItem> findAllByOrderByExpectedDateAscCreatedAtDesc();
}
