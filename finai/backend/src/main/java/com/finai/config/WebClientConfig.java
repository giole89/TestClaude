package com.finai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configura i client HTTP reattivi (WebClient) usati per:
 * <ul>
 *   <li>Yahoo Finance API (quote, storico, ricerca)</li>
 *   <li>NASDAQ IPO calendar API</li>
 *   <li>Anthropic Claude API (streaming SSE)</li>
 * </ul>
 *
 * <p>Ogni bean ha timeout e header di default appropriati al servizio.</p>
 */
@Configuration
public class WebClientConfig {

    @Value("${finai.yahoo.timeout-seconds:10}")
    private int yahooTimeoutSeconds;

    @Value("${finai.nasdaq.timeout-seconds:15}")
    private int nasdaqTimeoutSeconds;

    @Value("${finai.anthropic.api-key:}")
    private String anthropicApiKey;

    @Value("${finai.anthropic.version:2023-06-01}")
    private String anthropicVersion;

    /**
     * WebClient per Yahoo Finance. Imposta User-Agent e Accept per mimare
     * una richiesta browser (necessario per non ricevere 429 da Yahoo).
     */
    @Bean("yahooWebClient")
    public WebClient yahooWebClient(WebClient.Builder builder) {
        return builder
                .clientConnector(buildConnector(yahooTimeoutSeconds))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
                .build();
    }

    /**
     * WebClient per la NASDAQ IPO API. Richiede header Referer/Origin specifici
     * per superare la protezione browser-only dell'endpoint pubblico NASDAQ.
     */
    @Bean("nasdaqWebClient")
    public WebClient nasdaqWebClient(WebClient.Builder builder) {
        return builder
                .clientConnector(buildConnector(nasdaqTimeoutSeconds))
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Referer", "https://www.nasdaq.com/")
                .defaultHeader("Origin", "https://www.nasdaq.com")
                .build();
    }

    /**
     * WebClient per Anthropic Claude API. Include gli header di autenticazione
     * e la versione API richiesta da Anthropic.
     */
    @Bean("anthropicWebClient")
    public WebClient anthropicWebClient(WebClient.Builder builder) {
        return builder
                .clientConnector(buildConnector(120)) // timeout generoso per streaming
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", anthropicApiKey)
                .defaultHeader("anthropic-version", anthropicVersion)
                .build();
    }

    /** Costruisce un ReactorClientHttpConnector con timeout configurabili. */
    private ReactorClientHttpConnector buildConnector(int timeoutSeconds) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));
        return new ReactorClientHttpConnector(httpClient);
    }
}
