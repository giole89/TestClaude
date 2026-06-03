package com.finai.controller;

import com.finai.dto.news.NewsItemDto;
import com.finai.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Endpoint per le news finanziarie da Yahoo Finance.
 */
@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "News finanziarie da Yahoo Finance")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Recupera le news per uno o più ticker.
     *
     * @param tickers lista ticker separati da virgola
     * @param count   numero massimo di notizie (default 10, max 20)
     */
    @GetMapping
    @Operation(summary = "News finanziarie per ticker")
    public ResponseEntity<List<NewsItemDto>> getNews(
            @RequestParam(name = "tickers") String tickers,
            @RequestParam(defaultValue = "10") int count) {

        List<String> tickerList = Arrays.stream(tickers.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(t -> !t.isBlank())
                .limit(10)
                .toList();

        int safeCount = Math.min(Math.max(1, count), 20);
        return ResponseEntity.ok(newsService.fetchNews(tickerList, safeCount));
    }
}
