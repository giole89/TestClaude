package com.finai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Gestisce l'autenticazione con Yahoo Finance.
 *
 * Yahoo Finance richiede un "crumb" token (ottenuto dopo aver accettato
 * i cookie di consenso su fc.yahoo.com) da aggiungere a ogni richiesta API.
 * Questa classe ottiene e cachea il crumb, rinnovandolo automaticamente
 * in caso di risposta 401 o dopo 1 ora.
 */
@Component
public class YahooCrumbProvider {

    private static final Logger log = LoggerFactory.getLogger(YahooCrumbProvider.class);
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/125.0.0.0 Safari/537.36";

    private final HttpClient client;
    private final CookieManager cookieManager;

    private volatile String crumb;
    private volatile long crumbExpiry = 0;

    public YahooCrumbProvider() {
        this.cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        this.client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    /**
     * Esegue una GET sull'URL fornito aggiungendo il crumb come query param.
     * In caso di risposta 401, rinnova il crumb e riprova una volta.
     */
    public String fetch(String url) throws Exception {
        ensureCrumb();
        String body = doGet(appendCrumb(url));
        if (isUnauthorized(body)) {
            log.warn("Yahoo 401 — rinnovo crumb e riprovo");
            forceRefresh();
            body = doGet(appendCrumb(url));
        }
        return body;
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void ensureCrumb() {
        if (crumb == null || System.currentTimeMillis() > crumbExpiry) {
            forceRefresh();
        }
    }

    private synchronized void forceRefresh() {
        try {
            // Accetta i cookie di consenso Yahoo
            doGet("https://fc.yahoo.com");
            // Ottieni il crumb token
            String body = doGet("https://query1.finance.yahoo.com/v1/test/getcrumb");
            if (body != null && !body.isBlank() && !body.startsWith("{")) {
                crumb = body.trim();
                crumbExpiry = System.currentTimeMillis() + 3_600_000L;
                log.info("Yahoo crumb aggiornato");
            } else {
                log.warn("Crumb Yahoo non ottenuto: {}", body);
            }
        } catch (Exception e) {
            log.warn("Errore refresh crumb Yahoo: {}", e.getMessage());
        }
    }

    private String doGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://finance.yahoo.com/")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    private String appendCrumb(String url) {
        if (crumb == null) return url;
        String enc = URLEncoder.encode(crumb, StandardCharsets.UTF_8);
        return url + (url.contains("?") ? "&" : "?") + "crumb=" + enc;
    }

    private boolean isUnauthorized(String body) {
        return body != null && body.contains("\"Unauthorized\"");
    }
}
