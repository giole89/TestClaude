package com.finai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finai.dto.news.NewsItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recupera news finanziarie da Yahoo Finance.
 *
 * <p>Prova prima l'API v8 search, poi fallback su RSS Yahoo Finance.</p>
 */
@Service
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title><!\\[CDATA\\[(.+?)\\]\\]></title>");
    private static final Pattern LINK_PATTERN = Pattern.compile("<link>(.+?)</link>");
    private static final Pattern PUB_DATE_PATTERN = Pattern.compile("<pubDate>(.+?)</pubDate>");
    private static final Pattern CREATOR_PATTERN = Pattern.compile("<dc:creator><!\\[CDATA\\[(.+?)\\]\\]></dc:creator>");
    private static final Pattern DESC_PATTERN = Pattern.compile("<description><!\\[CDATA\\[(.+?)\\]\\]></description>");

    private final YahooCrumbProvider crumb;
    private final ObjectMapper mapper;

    @Value("${finai.yahoo.base-url-v8:https://query1.finance.yahoo.com/v8/finance}")
    private String baseUrlV8;

    public NewsService(YahooCrumbProvider crumb, ObjectMapper mapper) {
        this.crumb  = crumb;
        this.mapper = mapper;
    }

    /**
     * Recupera news per uno o più ticker.
     *
     * @param tickers lista di ticker
     * @param count   numero massimo di notizie per ticker
     * @return lista di NewsItemDto
     */
    @Cacheable(value = "news", key = "#tickers.toString() + ':' + #count")
    public List<NewsItemDto> fetchNews(List<String> tickers, int count) {
        if (tickers == null || tickers.isEmpty()) return List.of();

        List<NewsItemDto> allNews = new ArrayList<>();
        int perTicker = Math.max(1, count / tickers.size() + 1);

        for (String ticker : tickers) {
            try {
                List<NewsItemDto> tickerNews = fetchForTicker(ticker, perTicker);
                allNews.addAll(tickerNews);
            } catch (Exception e) {
                log.debug("Errore news per {}: {}", ticker, e.getMessage());
            }
        }

        // Ordina per data decrescente e limita
        return allNews.stream()
                .sorted((a, b) -> {
                    long ta = a.publishedAt() != null ? a.publishedAt() : 0L;
                    long tb = b.publishedAt() != null ? b.publishedAt() : 0L;
                    return Long.compare(tb, ta);
                })
                .limit(count)
                .toList();
    }

    private List<NewsItemDto> fetchForTicker(String ticker, int count) {
        // Prima prova API Yahoo v8 news search
        try {
            return fetchViaApi(ticker, count);
        } catch (Exception e) {
            log.debug("API news fallita per {}, provo RSS: {}", ticker, e.getMessage());
        }
        // Fallback su RSS
        try {
            return fetchViaRss(ticker, count);
        } catch (Exception e) {
            log.debug("RSS news fallito per {}: {}", ticker, e.getMessage());
        }
        return List.of();
    }

    private List<NewsItemDto> fetchViaApi(String ticker, int count) throws Exception {
        String url = baseUrlV8 + "/search?q=" + ticker + "&newsCount=" + count + "&quotesCount=0";
        String body = crumb.fetch(url);
        JsonNode root = mapper.readTree(body);
        JsonNode items = root.path("news");

        List<NewsItemDto> result = new ArrayList<>();
        if (!items.isArray()) return result;

        for (JsonNode item : items) {
            String title = item.path("title").asText(null);
            String link  = item.path("link").asText(null);
            if (title == null || link == null) continue;

            String publisher  = item.path("publisher").asText(null);
            Long publishedAt  = nullableLong(item, "providerPublishTime");
            if (publishedAt != null) publishedAt *= 1000L; // converti a ms
            String summary    = item.path("summary").asText(null);

            result.add(new NewsItemDto(title, link, publisher, publishedAt, summary, ticker));
        }
        return result;
    }

    private List<NewsItemDto> fetchViaRss(String ticker, int count) throws Exception {
        String rssUrl = "https://feeds.finance.yahoo.com/rss/2.0/headline?s=" + ticker + "&region=US&lang=en-US";

        URL url = new URL(rssUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }

        String xml = sb.toString();
        return parseRss(xml, ticker, count);
    }

    private List<NewsItemDto> parseRss(String xml, String ticker, int count) {
        List<NewsItemDto> result = new ArrayList<>();

        // Split per <item>
        String[] items = xml.split("<item>");
        for (int i = 1; i < items.length && result.size() < count; i++) {
            String item = items[i];

            String title = extractFirst(TITLE_PATTERN, item);
            String link  = extractLink(item);
            if (title == null) continue;

            String publisher = extractFirst(CREATOR_PATTERN, item);
            Long publishedAt = parseRssDate(extractFirst(PUB_DATE_PATTERN, item));
            String summary   = extractFirst(DESC_PATTERN, item);

            result.add(new NewsItemDto(title, link, publisher, publishedAt, summary, ticker));
        }
        return result;
    }

    private String extractFirst(Pattern p, String text) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractLink(String item) {
        // <link> non ha CDATA
        Matcher m = LINK_PATTERN.matcher(item);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private Long parseRssDate(String dateStr) {
        if (dateStr == null) return null;
        try {
            // RFC 2822 format: "Thu, 01 Jan 2026 12:00:00 +0000"
            java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter
                    .ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH);
            return java.time.ZonedDateTime.parse(dateStr, fmt).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    private Long nullableLong(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asLong();
    }
}
