package com.finai.controller;

import com.finai.dto.quote.FullQuoteDto;
import com.finai.dto.quote.HistoryPoint;
import com.finai.dto.quote.QuoteDto;
import com.finai.exception.FinaiException;
import com.finai.service.IndicatorsService;
import com.finai.service.YahooFinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint per quote singole: base e full (con indicatori tecnici).
 */
@RestController
@RequestMapping("/api/quote")
@Tag(name = "Quote", description = "Quote singola con o senza indicatori tecnici")
public class QuoteController {

    private final YahooFinanceService yahoo;
    private final IndicatorsService   indicators;

    public QuoteController(YahooFinanceService yahoo, IndicatorsService indicators) {
        this.yahoo      = yahoo;
        this.indicators = indicators;
    }

    /**
     * Quote base senza indicatori tecnici.
     *
     * @param ticker simbolo Yahoo Finance (es. AAPL, ISP.MI)
     */
    @GetMapping("/{ticker}")
    @Operation(summary = "Quote base di un titolo")
    public ResponseEntity<QuoteDto> quote(@PathVariable String ticker) {
        QuoteDto dto = yahoo.fetchQuote(ticker.toUpperCase());
        if (dto == null) throw new FinaiException("Servizio temporaneamente non disponibile", 503);
        return ResponseEntity.ok(dto);
    }

    /**
     * Quote completa con indicatori tecnici e storico 1 anno.
     * Calcola RSI, SMA 20/50/200, volatilità, momentum e BullScore.
     */
    @GetMapping("/{ticker}/full")
    @Operation(summary = "Quote completa con indicatori tecnici e storico")
    public ResponseEntity<FullQuoteDto> fullQuote(@PathVariable String ticker) {
        String sym = ticker.toUpperCase();
        QuoteDto quote = yahoo.fetchQuote(sym);
        if (quote == null) throw new FinaiException("Servizio temporaneamente non disponibile", 503);

        List<HistoryPoint> history = yahoo.fetchHistory(sym, "1y");
        List<Double> closes = indicators.extractCloses(history);

        Integer rsi        = indicators.calcRsi(closes, 14);
        Double  sma20      = indicators.calcSma(closes, 20);
        Double  sma50      = indicators.calcSma(closes, 50);
        Double  sma200     = indicators.calcSma(closes, 200);
        Integer volatility = indicators.calcVolatility(closes);
        Double  momentum30 = indicators.calcMomentum(closes, 30);
        int     bullScore  = indicators.calcBullScore(
                quote.price(), sma20, sma50, sma200,
                rsi, momentum30, volatility, quote.rangePosition());

        return ResponseEntity.ok(new FullQuoteDto(
                quote.ticker(), quote.name(), quote.price(),
                quote.dayChange(), quote.dayChangePct(), quote.ytdChangePct(),
                quote.high52w(), quote.low52w(), quote.volume(),
                quote.marketCap(), quote.pe(), quote.currency(),
                quote.exchange(), quote.rangePosition(), quote.timestamp(),
                rsi, sma20, sma50, sma200, volatility, momentum30, bullScore,
                history));
    }
}
