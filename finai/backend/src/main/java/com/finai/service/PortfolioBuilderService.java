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
 * 60/40 (obbligazionario): viene ricalcolato risolvendo la formula chiusa del
 * portafoglio tangente a due asset della Modern Portfolio Theory di Markowitz,
 * usando rendimento e volatilità annualizzati di ciascun ETF su uno storico a
 * 3 anni (più robusto statisticamente di 1 anno solo) e la correlazione tra i
 * due strumenti (via {@link CorrelationService}): a differenza di un confronto
 * isolato tra Sharpe ratio, la formula tiene conto di come i due strumenti si
 * muovono l'uno rispetto all'altro, così una bassa correlazione può comunque
 * spingere a diversificare anche quando un singolo Sharpe ratio sembrerebbe
 * suggerire di concentrarsi su un solo strumento, entro limiti prudenziali che
 * evitano di snaturare la diversificazione del core.</p>
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

    private record Metrics(double annualizedReturn, double annualizedVolatility, double sharpeRatio,
                            double[] dailyReturns, boolean reliable) {}

    private final YahooFinanceService yahoo;
    private final CorrelationService correlation;

    public PortfolioBuilderService(YahooFinanceService yahoo, CorrelationService correlation) {
        this.yahoo = yahoo;
        this.correlation = correlation;
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
                double coreWeightShare = markowitzCoreShare(coreMetrics, satMetrics, EQUITY_CORE_DEFAULT, EQUITY_CORE_MIN, EQUITY_CORE_MAX);

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
                double coreWeightShare = markowitzCoreShare(coreMetrics, satMetrics, BOND_CORE_DEFAULT, BOND_CORE_MIN, BOND_CORE_MAX);

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

    // ───────────────────────── Pesatura statistica (Markowitz a 2 asset) ──────

    private static final String HISTORY_RANGE = "3y";
    /** Minimo di ritorni giornalieri richiesti su uno storico ~3y (≈750 giorni di borsa) perché la stima sia considerata attendibile. */
    private static final int MIN_RELIABLE_RETURNS = 150;

    /**
     * Calcola rendimento, volatilità annualizzati e Sharpe ratio di un ticker
     * sullo storico daily a 3 anni (più stabile statisticamente di una
     * finestra a 1 anno, che su pochi mesi può essere dominata da rumore di
     * breve periodo). Se i dati sono insufficienti (errore di rete, ticker
     * nuovo, ecc.) restituisce {@code reliable=false}: in quel caso il
     * chiamante ricade sullo split predefinito.
     */
    private Metrics metricsFor(String ticker) {
        List<HistoryPoint> history = yahoo.fetchHistory(ticker, HISTORY_RANGE);
        double[] dailyReturns = correlation.calcDailyReturns(history);
        if (dailyReturns.length < MIN_RELIABLE_RETURNS) return new Metrics(0, 0, 0, dailyReturns, false);

        double meanDaily = java.util.Arrays.stream(dailyReturns).average().orElse(0);
        double variance = java.util.Arrays.stream(dailyReturns).map(d -> Math.pow(d - meanDaily, 2)).average().orElse(0);
        double dailyVol = Math.sqrt(variance);

        double annualizedReturn = meanDaily * TRADING_DAYS_PER_YEAR;
        double annualizedVolatility = dailyVol * Math.sqrt(TRADING_DAYS_PER_YEAR);
        double sharpe = annualizedVolatility > 0 ? (annualizedReturn - RISK_FREE_RATE) / annualizedVolatility : 0;

        return new Metrics(annualizedReturn, annualizedVolatility, sharpe, dailyReturns, true);
    }

    /**
     * Quota da assegnare al "core" entro [min, max], calcolata risolvendo la
     * formula chiusa del portafoglio tangente (massimo Sharpe ratio) per due
     * asset della Modern Portfolio Theory:
     * <pre>
     * w_core = [(r_core-rf)·σ_sat² - (r_sat-rf)·ρ·σ_core·σ_sat]
     *        / [(r_core-rf)·σ_sat² + (r_sat-rf)·σ_core² - ((r_core-rf)+(r_sat-rf))·ρ·σ_core·σ_sat]
     * </pre>
     * A differenza di un confronto isolato tra Sharpe ratio, questa formula
     * incorpora anche la correlazione ρ tra i due strumenti: una bassa o
     * negativa correlazione riduce il rischio combinato e quindi può spingere
     * verso una maggiore diversificazione anche quando un singolo Sharpe
     * ratio premierebbe la concentrazione. Il peso risultante resta comunque
     * vincolato entro [min, max] per non snaturare la diversificazione del
     * core, e ricade sul default se le metriche non sono attendibili o se la
     * formula degenera (denominatore vicino a zero).
     */
    private double markowitzCoreShare(Metrics core, Metrics satellite, double defaultShare, double min, double max) {
        if (!core.reliable() || !satellite.reliable()) return defaultShare;

        double rho = pairwiseCorrelation(core, satellite);

        double excessCore = core.annualizedReturn() - RISK_FREE_RATE;
        double excessSat = satellite.annualizedReturn() - RISK_FREE_RATE;
        double varCore = core.annualizedVolatility() * core.annualizedVolatility();
        double varSat = satellite.annualizedVolatility() * satellite.annualizedVolatility();
        double covCoreSat = rho * core.annualizedVolatility() * satellite.annualizedVolatility();

        double numerator = excessCore * varSat - excessSat * covCoreSat;
        double denominator = excessCore * varSat + excessSat * varCore - (excessCore + excessSat) * covCoreSat;

        if (Math.abs(denominator) < 1e-9) return defaultShare;

        double share = numerator / denominator;
        if (Double.isNaN(share) || Double.isInfinite(share)) return defaultShare;
        return Math.min(Math.max(share, min), max);
    }

    /** Correlazione di Pearson tra i ritorni giornalieri di core e satellite, allineati alla lunghezza comune minima. */
    private double pairwiseCorrelation(Metrics core, Metrics satellite) {
        int len = Math.min(core.dailyReturns().length, satellite.dailyReturns().length);
        if (len < 2) return 0.0;
        double[] a = java.util.Arrays.copyOf(core.dailyReturns(), len);
        double[] b = java.util.Arrays.copyOf(satellite.dailyReturns(), len);
        return correlation.pearson(a, b);
    }

    private String statRationale(Metrics core, Metrics satellite, double coreShare, boolean isCore) {
        if (!core.reliable() || !satellite.reliable()) return "";
        String pesoLabel = isCore ? "core" : "satellite";
        double sharpeShown = isCore ? core.sharpeRatio() : satellite.sharpeRatio();
        double weightShown = isCore ? coreShare : 1 - coreShare;
        double rho = pairwiseCorrelation(core, satellite);
        return String.format(Locale.ITALIAN,
                " — Sharpe a 3 anni %.2f, correlazione core/satellite %.2f (peso %s %.0f%%, da formula del portafoglio tangente di Markowitz)",
                sharpeShown, rho, pesoLabel, weightShown * 100);
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
