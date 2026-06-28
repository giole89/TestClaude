package com.finai.service;

import com.finai.dto.finance.AllocationDto;
import com.finai.dto.finance.MarketSnapshotDto;
import com.finai.dto.finance.PortfolioLineDto;
import com.finai.dto.quote.QuoteDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce l'allocazione percentuale (azionario/obbligazionario/liquidità) del
 * questionario in un portafoglio concreto fatto di ETF UCITS reali, usando le
 * quote di oggi per renderlo verosimile, e aggiunge una lettura sintetica
 * dell'andamento del mercato del giorno (S&P 500 + VIX).
 *
 * <p>È un esempio illustrativo a scopo informativo, non una raccomandazione
 * di acquisto: gli strumenti sono scelti da un paniere fisso di ETF noti e
 * liquidi, non da un'analisi di convenienza del momento.</p>
 */
@Service
public class PortfolioBuilderService {

    public record Result(MarketSnapshotDto snapshot, List<PortfolioLineDto> portfolio) {}

    private static final String SP500 = "^GSPC";
    private static final String VIX = "^VIX";

    private static final String EQUITY_CORE = "VWCE.DE";
    private static final String EQUITY_CORE_NAME = "Vanguard FTSE All-World UCITS ETF";
    private static final String EQUITY_SATELLITE_GROWTH = "QQQ";
    private static final String EQUITY_SATELLITE_GROWTH_NAME = "Invesco Nasdaq-100 ETF";
    private static final String EQUITY_SATELLITE_DIVERSIFY = "EIMI.AS";
    private static final String EQUITY_SATELLITE_DIVERSIFY_NAME = "iShares Core MSCI EM IMI UCITS ETF";
    private static final String BOND_CORE = "AGGH.AS";
    private static final String BOND_CORE_NAME = "iShares Core Global Aggregate Bond UCITS ETF";
    private static final String BOND_SATELLITE = "IEAG.AS";
    private static final String BOND_SATELLITE_NAME = "iShares Euro Aggregate Bond UCITS ETF";
    private static final String LIQUIDITY = "IB01.AS";
    private static final String LIQUIDITY_NAME = "iShares $ Treasury Bond 0-1yr UCITS ETF";

    private final YahooFinanceService yahoo;

    public PortfolioBuilderService(YahooFinanceService yahoo) {
        this.yahoo = yahoo;
    }

    public Result build(AllocationDto allocation, String goal) {
        boolean growthSatellite = "GROWTH".equals(goal) || "RETIREMENT".equals(goal);

        List<String> tickers = new ArrayList<>(List.of(SP500, VIX));
        if (allocation.equityPct() > 0) {
            tickers.add(EQUITY_CORE);
            if (allocation.equityPct() >= 20) {
                tickers.add(growthSatellite ? EQUITY_SATELLITE_GROWTH : EQUITY_SATELLITE_DIVERSIFY);
            }
        }
        if (allocation.bondPct() > 0) {
            tickers.add(BOND_CORE);
            if (allocation.bondPct() >= 20) tickers.add(BOND_SATELLITE);
        }
        if (allocation.liquidityPct() > 0) {
            tickers.add(LIQUIDITY);
        }

        Map<String, QuoteDto> quotes = yahoo.fetchBatch(tickers).stream()
                .collect(Collectors.toMap(q -> q.ticker().toUpperCase(Locale.ROOT), q -> q, (a, b) -> a));

        MarketSnapshotDto snapshot = buildSnapshot(quotes.get(SP500), quotes.get(VIX));
        List<PortfolioLineDto> portfolio = new ArrayList<>();

        if (allocation.equityPct() > 0) {
            if (allocation.equityPct() >= 20) {
                double core = round1(allocation.equityPct() * 0.7);
                double satellite = round1(allocation.equityPct() - core);
                portfolio.add(line(quotes, EQUITY_CORE, EQUITY_CORE_NAME, "Azionario", core,
                        "Core azionario globale diversificato su 3700+ titoli"));
                String satTicker = growthSatellite ? EQUITY_SATELLITE_GROWTH : EQUITY_SATELLITE_DIVERSIFY;
                String satName = growthSatellite ? EQUITY_SATELLITE_GROWTH_NAME : EQUITY_SATELLITE_DIVERSIFY_NAME;
                String satWhy = growthSatellite
                        ? "Satellite growth — esposizione ai titoli tech ad alta crescita"
                        : "Satellite mercati emergenti — diversifica la componente azionaria";
                portfolio.add(line(quotes, satTicker, satName, "Azionario", satellite, satWhy));
            } else {
                portfolio.add(line(quotes, EQUITY_CORE, EQUITY_CORE_NAME, "Azionario", round1(allocation.equityPct()),
                        "Quota azionaria contenuta vista la finestra temporale breve"));
            }
        }

        if (allocation.bondPct() > 0) {
            if (allocation.bondPct() >= 20) {
                double core = round1(allocation.bondPct() * 0.6);
                double satellite = round1(allocation.bondPct() - core);
                portfolio.add(line(quotes, BOND_CORE, BOND_CORE_NAME, "Obbligazionario", core,
                        "Obbligazioni globali investment grade — stabilità e cedole"));
                portfolio.add(line(quotes, BOND_SATELLITE, BOND_SATELLITE_NAME, "Obbligazionario", satellite,
                        "Obbligazioni euro — riduce il rischio di cambio"));
            } else {
                portfolio.add(line(quotes, BOND_CORE, BOND_CORE_NAME, "Obbligazionario", round1(allocation.bondPct()),
                        "Obbligazioni globali investment grade"));
            }
        }

        if (allocation.liquidityPct() > 0) {
            portfolio.add(line(quotes, LIQUIDITY, LIQUIDITY_NAME, "Liquidità", round1(allocation.liquidityPct()),
                    "Liquidità/equivalenti a brevissimo termine — capitale protetto e disponibile"));
        }

        return new Result(snapshot, portfolio);
    }

    private PortfolioLineDto line(Map<String, QuoteDto> quotes, String ticker, String fallbackName, String assetClass,
                                   double weightPct, String rationale) {
        QuoteDto q = quotes.get(ticker.toUpperCase(Locale.ROOT));
        Double price = q != null ? q.price() : null;
        Double change = q != null ? q.dayChangePct() : null;
        String currency = q != null ? q.currency() : null;
        String name = q != null && q.name() != null ? q.name() : fallbackName;
        return new PortfolioLineDto(ticker, name, assetClass, weightPct, price, change, currency, rationale);
    }

    private MarketSnapshotDto buildSnapshot(QuoteDto sp500, QuoteDto vix) {
        Double spChange = sp500 != null ? sp500.dayChangePct() : null;
        Double vixLevel = vix != null ? vix.price() : null;

        String trend = spChange == null ? "stabile"
                : spChange > 0.15 ? "in rialzo" : spChange < -0.15 ? "in ribasso" : "stabile";
        String vol = vixLevel == null ? null
                : vixLevel < 15 ? "bassa volatilità" : vixLevel <= 25 ? "volatilità nella norma" : "alta volatilità / nervosismo";
        String sentiment = vol == null ? trend : trend + ", " + vol;

        String note = String.format(Locale.ITALIAN,
                "Oggi l'S&P 500 è %s (%s%.2f%%) e il VIX è a %s (%s). Il portafoglio sotto usa le quote attuali di mercato, a parità di allocazione decisa in base al tuo profilo: non è un consiglio di investimento personalizzato, ma un esempio concreto per orientarti.",
                trend,
                spChange != null && spChange > 0 ? "+" : "",
                spChange != null ? spChange : 0.0,
                vixLevel != null ? String.format(Locale.ITALIAN, "%.1f", vixLevel) : "n/d",
                vol != null ? vol : "dato non disponibile");

        return new MarketSnapshotDto(spChange, vixLevel, sentiment, note);
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
