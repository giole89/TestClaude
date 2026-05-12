package com.finai.repository;

import com.finai.domain.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository per le posizioni di portafoglio.
 * Usa Spring Data JPA: nessun SQL scritto manualmente eccetto dove ottimizzazione richiesta.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioItem, String> {

    /** Restituisce tutte le posizioni ordinate per data di creazione (più recenti prima). */
    List<PortfolioItem> findAllByOrderByCreatedAtDesc();

    /** Aggiorna il prezzo corrente e la valuta per tutti i titoli con il ticker dato. */
    @Modifying
    @Query("UPDATE PortfolioItem p SET p.currentPrice = :price, p.currency = :currency " +
           "WHERE p.ticker = :ticker")
    int updatePriceByTicker(@Param("ticker") String ticker,
                            @Param("price")  BigDecimal price,
                            @Param("currency") String currency);

    /** Aggiorna il nome per tutti i titoli con il ticker dato. */
    @Modifying
    @Query("UPDATE PortfolioItem p SET p.name = :name WHERE p.ticker = :ticker")
    int updateNameByTicker(@Param("ticker") String ticker, @Param("name") String name);
}
