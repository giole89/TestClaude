package com.finai.service;

import com.finai.dto.finance.AllocationDto;
import com.finai.dto.finance.MarketSnapshotDto;
import com.finai.dto.finance.PortfolioLineDto;
import com.finai.dto.quote.HistoryPoint;
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
 * <p>L'allocazione macro (quanto azionario/obbligazionario/liquidità) resta
 * decisa dal profilo dell'investitore (obiettivo, orizzonte temporale), perché
 * riflette la capacità di rischio della persona, non la convenienza di un
 * singolo strumento. Il peso tra strumento "core" e "satellite" all'interno
 * di ciascun macro-bucket, invece, non è più un fisso 70/30 (azionario) o
 * 60/40 (obbligazionario): viene ricalcolato in base allo storico a 1 anno di
 * rendimento e volatilità di ciascun ETF, usando lo Sharpe ratio
 * (rendimento in eccesso rispetto a un tasso privo di rischio, diviso la
 * volatilità) come misura di rendimento aggiustato per il rischio — l'idea
 * alla base della Modern Portfolio Theory di Markowitz e dello Sharpe ratio
 * di William Sharpe: a parità di rischio "macro" già fissato dal profilo,
 * si tende a pesare di più lo strumento con il miglior rapporto
 * rendimento/rischio recente, entro limiti prudenziali che evitano di
 * snaturare la diversificazione del core.</p>
 *
 * <p>È un esempio illustrativo a scopo informativo, non una raccomandazione
 * di acquisto personalizzata.</p>
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

    /** Tasso privo di rischio annuo usato come riferimento per lo Sharpe ratio. */
    private static final double RISK_FREE_RATE = 0.02;
    private static final int TRADING_DAYS_PER_YEAR = 252;

    private static final double EQUITY_CORE_DEFAULT = 0.7;
    private static final double EQUITY_CORE_MIN = 0.5;
    private static final double EQUITY_CORE_MAX = 0.85;
    private static final double BOND_CORE_DEFAULT = 0.6;
    private static final double BOND_CORE_MIN = 0.4;
    private static final double BOND_CORE_MAX = 0.75;

    private record Metrics(double annualizedReturn, double annualizedVolatility, double sharpeRatio, boolean reliable) {}

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
                String satTicker = growthSatellite ? EQUITY_SATELLITE_GROWTH : EQUITY_SATELLITE_DIVERSIFY;
                String satName = growthSatellite ? EQUITY_SATELLITE_GROWTH_NAME : EQUITY_SATELLITE_DIVERSIFY_NAME;
                String satWhyBase = growthSatellite
                        ? "Satellite growth — esposizione ai titoli tech ad alta crescita"
                        : "Satellite mercati emergenti — diversifica la componente azionaria";

                Metrics coreMetrics = metricsFor(EQUITY_CORE);
                Metrics satMetrics = metricsFor(satTicker);
                double coreWeightShare = sharpeWeightedShare(coreMetrics, satMetrics, EQUITY_CORE_DEFAULT, EQUITY_CORE_MIN, EQUITY_CORE_MAX);

                double core = round1(allocation.equityPct() * coreWeightShare);
                double satellite = round1(allocation.equityPct() - core);

                portfolio.add(line(quotes, EQUITY_CORE, EQUITY_CORE_NAME, "Azionario", core,
                        "Core azionario globale diversificato su 3700+ titoli" + statRationale(coreMetrics, satMetrics, coreWeightShare, true)));
                portfolio.add(line(quotes, satTicker, satName, "Azionario", satellite,
                        satWhyBase + statRationale(coreMetrics, satMetrics, coreWeightShare, false)));
            } else {
                portfolio.add(line(quotes, EQUITY_CORE, EQUITY_CORE_NAME, "Azionario", round1(allocation.equityPct()),
                        "Quota azionaria contenuta vista la finestra temporale breve"));
            }
        }

        if (allocation.bondPct() > 0) {
            if (allocation.bondPct() >= 20) {
                Metrics coreMetrics = metricsFor(BOND_CORE);
                Metrics satMetrics = metricsFor(BOND_SATELLITE);
                double coreWeightShare = sharpeWeightedShare(coreMetrics, satMetrics, BOND_CORE_DEFAULT, BOND_CORE_MIN, BOND_CORE_MAX);

                double core = round1(allocation.bondPct() * coreWeightShare);
                double satellite = round1(allocation.bondPct() - core);

                portfolio.add(line(quotes, BOND_CORE, BOND_CORE_NAME, "Obbligazionario", core,
                        "Obbligazioni globali investment grade — stabilità e cedole" + statRationale(coreMetrics, satMetrics, coreWeightShare, true)));
                portfolio.add(line(quotes, BOND_SATELLITE, BOND_SATELLITE_NAME, "Obbligazionario", satellite,
                        "Obbligazioni euro — riduce il rischio di cambio" + statRationale(coreMetrics, satMetrics, coreWeightShare, false)));
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

    // ─────────────────────────────── Pesatura statistica (Sharpe) ─────────────

    /**
     * Calcola rendimento, volatilità annualizzati e Sharpe ratio di un ticker
     * sullo storico daily a 1 anno. Se i dati sono insufficienti (errore di
     * rete, ticker nuovo, ecc.) restituisce {@code reliable=false}: in quel
     * caso il chiamante ricade sullo split predefinito.
     */
    private Metrics metricsFor(String ticker) {
        List<HistoryPoint> history = yahoo.fetchHistory(ticker, "1y");
        List<Double> dailyReturns = new ArrayList<>();
        for (int i = 1; i < history.size(); i++) {
            Double prev = history.get(i - 1).close();
            Double curr = history.get(i).close();
            if (prev != null && curr != null && prev > 0) {
                dailyReturns.add((curr - prev) / prev);
            }
        }
        if (dailyReturns.size() < 60) return new Metrics(0, 0, 0, false);

        double meanDaily = dailyReturns.stream().mapToDouble(d -> d).average().orElse(0);
        double variance = dailyReturns.stream().mapToDouble(d -> Math.pow(d - meanDaily, 2)).average().orElse(0);
        double dailyVol = Math.sqrt(variance);

        double annualizedReturn = meanDaily * TRADING_DAYS_PER_YEAR;
        double annualizedVolatility = dailyVol * Math.sqrt(TRADING_DAYS_PER_YEAR);
        double sharpe = annualizedVolatility > 0 ? (annualizedReturn - RISK_FREE_RATE) / annualizedVolatility : 0;

        return new Metrics(annualizedReturn, annualizedVolatility, sharpe, true);
    }

    /**
     * Quota da assegnare al "core" entro [min, max], spostata rispetto al
     * default in base al confronto tra gli Sharpe ratio di core e satellite:
     * lo strumento con il miglior rendimento aggiustato per il rischio pesa
     * di più, senza mai sbilanciare oltre i limiti prudenziali.
     */
    private double sharpeWeightedShare(Metrics core, Metrics satellite, double defaultShare, double min, double max) {
        if (!core.reliable() || !satellite.reliable()) return defaultShare;

        // Sposta gli Sharpe ratio su un dominio positivo per poterli usare come pesi proporzionali.
        double a = Math.max(core.sharpeRatio(), 0.05);
        double b = Math.max(satellite.sharpeRatio(), 0.05);
        double share = a / (a + b);
        return Math.min(Math.max(share, min), max);
    }

    private String statRationale(Metrics core, Metrics satellite, double coreShare, boolean isCore) {
        if (!core.reliable() || !satellite.reliable()) return "";
        String pesoLabel = isCore ? "core" : "satellite";
        double sharpeShown = isCore ? core.sharpeRatio() : satellite.sharpeRatio();
        double weightShown = isCore ? coreShare : 1 - coreShare;
        return String.format(Locale.ITALIAN,
                " — Sharpe a 1 anno %.2f (peso %s %.0f%%, calibrato sul rendimento aggiustato per il rischio)",
                sharpeShown, pesoLabel, weightShown * 100);
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
                "Oggi l'S&P 500 è %s (%s%.2f%%) e il VIX è a %s (%s). Il portafoglio sotto usa le quote attuali di mercato, a parità di allocazione decisa in base al tuo profilo, con i pesi core/satellite calibrati sullo storico di rendimento e volatilità a 1 anno: non è un consiglio di investimento personalizzato, ma un esempio concreto per orientarti.",
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
