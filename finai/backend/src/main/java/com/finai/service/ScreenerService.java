package com.finai.service;

import com.finai.dto.quote.QuoteDto;
import com.finai.dto.screener.ScreenerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Screener azionario su un universe fisso di ~80 ticker chiave.
 *
 * <p>Chiama fetchBatch su Yahoo Finance e filtra i risultati
 * secondo i criteri passati in input.</p>
 */
@Service
public class ScreenerService {

    private static final Logger log = LoggerFactory.getLogger(ScreenerService.class);

    // Universe fisso: US top 30 + IT + DE top 15 + ETF top 20
    private static final List<String> UNIVERSE = List.of(
            // US top 30
            "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "BRK-B", "JPM", "V",
            "UNH", "XOM", "LLY", "JNJ", "WMT", "MA", "PG", "HD", "MRK", "AVGO",
            "CVX", "PEP", "COST", "ABBV", "KO", "ADBE", "CSCO", "ACN", "MCD", "CRM",
            // IT
            "ISP.MI", "ENI.MI", "ENEL.MI", "UCG.MI", "STM.MI",
            "RACE.MI", "MONC.MI", "LDO.MI", "STLA.MI", "MB.MI",
            // DE top 15
            "SAP.DE", "SIE.DE", "ALV.DE", "DTE.DE", "BAYN.DE",
            "BMW.DE", "MBG.DE", "ADS.DE", "BASF.DE", "VOW3.DE",
            "DBK.DE", "RWE.DE", "MUV2.DE", "IFX.DE", "RHM.DE",
            // FR top 10
            "MC.PA", "OR.PA", "TTE.PA", "BNP.PA", "AIR.PA",
            "SU.PA", "AI.PA", "RMS.PA", "ENGI.PA", "ORA.PA",
            // ETF top 20
            "VWCE.DE", "IWDA.AS", "CSPX.AS", "EQQQ.AS",
            "SPY", "QQQ", "IVV", "VOO", "VTI", "VIG",
            "SCHD", "XLK", "XLF", "XLE", "XLV",
            "AGGH.AS", "SGLD.AS", "EIMI.AS", "WSML.AS", "IUIT.AS"
    );

    private final YahooFinanceService yahooFinanceService;

    public ScreenerService(YahooFinanceService yahooFinanceService) {
        this.yahooFinanceService = yahooFinanceService;
    }

    /**
     * Esegue lo screening con i filtri specificati.
     *
     * @param minPE     P/E minimo (null = nessun filtro)
     * @param maxPE     P/E massimo (null = nessun filtro)
     * @param minYield  Dividend yield minimo in % (null = nessun filtro)
     * @param minYtd    YTD change minimo % (null = nessun filtro)
     * @param market    Mercato ("us", "it", "de", "fr", null = tutti)
     * @param limit     Numero massimo di risultati
     * @return lista di ScreenerDto filtrata e ordinata per market cap decrescente
     */
    @Cacheable(value = "screener",
               key = "#minPE + ':' + #maxPE + ':' + #minYield + ':' + #minYtd + ':' + #market + ':' + #limit")
    public List<ScreenerDto> screen(Double minPE, Double maxPE, Double minYield,
                                    Double minYtd, String market, int limit) {
        // Partiziona l'universe in chunk da 20 per Yahoo Finance
        List<String> universe = filterByMarket(market);
        List<ScreenerDto> results = new ArrayList<>();

        List<List<String>> chunks = partition(universe, 20);
        for (List<String> chunk : chunks) {
            try {
                List<QuoteDto> quotes = yahooFinanceService.fetchBatch(chunk);
                for (QuoteDto q : quotes) {
                    ScreenerDto dto = toScreenerDto(q);
                    if (passesFilters(dto, minPE, maxPE, minYield, minYtd)) {
                        results.add(dto);
                    }
                }
            } catch (Exception e) {
                log.warn("Errore screener chunk {}: {}", chunk, e.getMessage());
            }
        }

        return results.stream()
                .sorted(Comparator.comparingLong(s -> -(s.marketCap() != null ? s.marketCap() : 0L)))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<String> filterByMarket(String market) {
        if (market == null || market.isBlank() || "all".equalsIgnoreCase(market)) {
            return UNIVERSE;
        }
        return UNIVERSE.stream()
                .filter(t -> matchesMarket(t, market))
                .collect(Collectors.toList());
    }

    private boolean matchesMarket(String ticker, String market) {
        return switch (market.toLowerCase()) {
            case "us"  -> !ticker.contains(".") && !ticker.contains("=");
            case "it"  -> ticker.endsWith(".MI");
            case "de"  -> ticker.endsWith(".DE");
            case "fr"  -> ticker.endsWith(".PA");
            case "nl"  -> ticker.endsWith(".AS");
            default    -> true;
        };
    }

    private boolean passesFilters(ScreenerDto s, Double minPE, Double maxPE,
                                  Double minYield, Double minYtd) {
        if (minPE != null && (s.pe() == null || s.pe() < minPE)) return false;
        if (maxPE != null && (s.pe() == null || s.pe() > maxPE)) return false;
        if (minYield != null) {
            Double yieldPct = s.dividendYield() != null ? s.dividendYield() * 100 : null;
            if (yieldPct == null || yieldPct < minYield) return false;
        }
        if (minYtd != null && (s.ytdChangePct() == null || s.ytdChangePct() < minYtd)) return false;
        return true;
    }

    private ScreenerDto toScreenerDto(QuoteDto q) {
        return new ScreenerDto(
                q.ticker(),
                q.name(),
                q.price(),
                q.pe(),
                null,  // dividendYield not in QuoteDto; screener shows what is available
                q.ytdChangePct(),
                q.high52w(),
                q.low52w(),
                q.marketCap(),
                q.exchange(),
                q.rangePosition(),
                q.currency()
        );
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
