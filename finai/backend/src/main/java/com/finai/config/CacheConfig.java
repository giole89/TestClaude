package com.finai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache multi-tier con TTL differenziati per tipologia di dato.
 *
 * <p>Ogni cache usa Caffeine con eviction LRU e TTL fisso dopo la scrittura.
 * I TTL sono allineati agli staleTime di React Query sul frontend.</p>
 *
 * <pre>
 * Cache       TTL    Motivazione
 * ─────────────────────────────────────────────────────────
 * quotes      60s    Dati real-time: si aggiornano ogni minuto
 * batch       120s   Batch market: leggermente più stabili
 * history     4h     Storico: non cambia durante la giornata
 * search      30s    Autocomplete: bassa latenza
 * ipo         1h     Calendario IPO: aggiornato raramente
 * ai          30m    Risposte AI: riusabili per stesso contesto
 * earnings    6h     Date earnings: cambiano raramente
 * dividends   24h    Dati dividendi: aggiornamento giornaliero
 * screener    5m     Screener: dati semi-freschi
 * news        15m    News: aggiornamento frequente
 * benchmark   1h     Benchmark vs S&P: storico stabile
 * correlation 4h     Correlazione: basata su history 1y
 * watchlist   60s    Watchlist: sincronizzazione rapida
 * </pre>
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.registerCustomCache("quotes",
                Caffeine.newBuilder().maximumSize(500).expireAfterWrite(60, TimeUnit.SECONDS).build());

        manager.registerCustomCache("batch",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(120, TimeUnit.SECONDS).build());

        manager.registerCustomCache("history",
                Caffeine.newBuilder().maximumSize(300).expireAfterWrite(4, TimeUnit.HOURS).build());

        manager.registerCustomCache("search",
                Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(30, TimeUnit.SECONDS).build());

        manager.registerCustomCache("ipo",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(1, TimeUnit.HOURS).build());

        manager.registerCustomCache("ai",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(30, TimeUnit.MINUTES).build());

        manager.registerCustomCache("earnings",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(6, TimeUnit.HOURS).build());

        manager.registerCustomCache("dividends",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(24, TimeUnit.HOURS).build());

        manager.registerCustomCache("screener",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(5, TimeUnit.MINUTES).build());

        manager.registerCustomCache("news",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(15, TimeUnit.MINUTES).build());

        manager.registerCustomCache("benchmark",
                Caffeine.newBuilder().maximumSize(100).expireAfterWrite(1, TimeUnit.HOURS).build());

        manager.registerCustomCache("correlation",
                Caffeine.newBuilder().maximumSize(50).expireAfterWrite(4, TimeUnit.HOURS).build());

        manager.registerCustomCache("watchlist",
                Caffeine.newBuilder().maximumSize(200).expireAfterWrite(60, TimeUnit.SECONDS).build());

        return manager;
    }
}
