package com.finai.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Interceptor di rate limiting basato su token bucket in memoria.
 *
 * <p>Applica due livelli di limite per IP:
 * <ul>
 *   <li><b>Globale</b>: 100 req/min per qualsiasi endpoint {@code /api/**}</li>
 *   <li><b>AI</b>: 20 req/min specificamente per {@code /api/ai/**}</li>
 * </ul>
 *
 * <p>Il bucket viene resettato ogni finestra di 60 secondi.
 * In un sistema distribuito sostituire con Redis + Bucket4j.</p>
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final long WINDOW_MS = 60_000L;

    @Value("${finai.rate-limit.global-per-minute:100}")
    private int globalLimit;

    @Value("${finai.rate-limit.ai-per-minute:20}")
    private int aiLimit;

    /** Contatori per il limite globale: chiave = IP. */
    private final ConcurrentHashMap<String, BucketEntry> globalBuckets = new ConcurrentHashMap<>();

    /** Contatori per il limite AI: chiave = IP. */
    private final ConcurrentHashMap<String, BucketEntry> aiBuckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {

        String ip = extractIp(request);
        String path = request.getRequestURI();

        // Controlla limite AI prima del globale
        if (path.startsWith("/api/ai")) {
            if (!allow(aiBuckets, ip, aiLimit)) {
                log.warn("Rate limit AI superato per IP={}", ip);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit AI: max " + aiLimit + " req/min\"}");
                return false;
            }
        }

        if (!allow(globalBuckets, ip, globalLimit)) {
            log.warn("Rate limit globale superato per IP={}", ip);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit: max " + globalLimit + " req/min\"}");
            return false;
        }

        return true;
    }

    private boolean allow(ConcurrentHashMap<String, BucketEntry> buckets, String ip, int limit) {
        long now = System.currentTimeMillis();
        BucketEntry entry = buckets.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new BucketEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return entry.count.get() <= limit;
    }

    private String extractIp(HttpServletRequest request) {
        // X-Forwarded-For è trusted solo se la connessione arriva da localhost/reverse proxy interno.
        // In tutti gli altri casi usiamo getRemoteAddr() per evitare IP spoofing.
        String remoteAddr = request.getRemoteAddr();
        boolean isTrustedProxy = "127.0.0.1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr)
                || remoteAddr.startsWith("172.") // Docker bridge networks
                || remoteAddr.startsWith("10.");
        if (isTrustedProxy) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    private record BucketEntry(long windowStart, AtomicInteger count) {}
}
