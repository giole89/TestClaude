package com.finai.service;

import com.finai.dto.quote.HistoryPoint;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Calcola indicatori tecnici su serie storiche di prezzi.
 *
 * <p>Tutti i metodi operano su {@code List<Double>} di prezzi di chiusura
 * e sono stateless: nessuna dipendenza esterna, facilmente testabili in isolamento.</p>
 *
 * <p>Gli stessi algoritmi sono replicati nel frontend (TypeScript) per
 * i calcoli lato client (grafici, scenari). Il backend è la fonte autoritativa.</p>
 */
@Service
public class IndicatorsService {

    // ─────────────────────────────────── RSI ─────────────────────────────────

    /**
     * Calcola il Relative Strength Index a {@code period} periodi.
     *
     * <p>Usa la smoothing media di Wilder (EMA con α = 1/period).
     * Restituisce null se la serie è troppo corta.</p>
     *
     * @param closes prezzi di chiusura in ordine cronologico
     * @param period numero di periodi (tipicamente 14)
     * @return RSI in range [0, 100], oppure null
     */
    public Integer calcRsi(List<Double> closes, int period) {
        if (closes == null || closes.size() < period + 1) return null;

        double avgGain = 0, avgLoss = 0;
        for (int i = 1; i <= period; i++) {
            double delta = closes.get(i) - closes.get(i - 1);
            if (delta > 0) avgGain += delta;
            else           avgLoss -= delta;
        }
        avgGain /= period;
        avgLoss /= period;

        // Smoothing di Wilder per i periodi successivi al primo
        for (int i = period + 1; i < closes.size(); i++) {
            double delta = closes.get(i) - closes.get(i - 1);
            if (delta > 0) {
                avgGain = (avgGain * (period - 1) + delta) / period;
                avgLoss = (avgLoss * (period - 1)) / period;
            } else {
                avgGain = (avgGain * (period - 1)) / period;
                avgLoss = (avgLoss * (period - 1) - delta) / period;
            }
        }

        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return (int) Math.round(100 - (100 / (1 + rs)));
    }

    // ─────────────────────────────────── SMA ─────────────────────────────────

    /**
     * Calcola la Simple Moving Average sugli ultimi {@code period} valori.
     *
     * @return media aritmetica, oppure null se la serie è insufficiente
     */
    public Double calcSma(List<Double> closes, int period) {
        if (closes == null || closes.size() < period) return null;
        int start = closes.size() - period;
        double sum = 0;
        for (int i = start; i < closes.size(); i++) sum += closes.get(i);
        return sum / period;
    }

    // ─────────────────────────────────── Volatilità ───────────────────────────

    /**
     * Calcola la volatilità annualizzata come deviazione standard dei log-return giornalieri.
     *
     * <p>Formula: {@code √252 × σ(log(p[i]/p[i-1]))}</p>
     *
     * @return volatilità in percentuale (es. 24 = 24%), oppure null
     */
    public Integer calcVolatility(List<Double> closes) {
        if (closes == null || closes.size() < 20) return null;

        int n = closes.size();
        double[] returns = new double[n - 1];
        for (int i = 1; i < n; i++) {
            if (closes.get(i - 1) <= 0) continue;
            returns[i - 1] = Math.log(closes.get(i) / closes.get(i - 1));
        }

        double mean = 0;
        for (double r : returns) mean += r;
        mean /= returns.length;

        double variance = 0;
        for (double r : returns) variance += (r - mean) * (r - mean);
        variance /= (returns.length - 1);

        double annualized = Math.sqrt(variance * 252) * 100;
        return (int) Math.round(annualized);
    }

    // ─────────────────────────────────── Momentum ────────────────────────────

    /**
     * Calcola il momentum come variazione percentuale sull'ultimo periodo.
     *
     * @param days numero di giorni del periodo (es. 30)
     * @return variazione %, oppure null
     */
    public Double calcMomentum(List<Double> closes, int days) {
        if (closes == null || closes.size() < days + 1) return null;
        int n = closes.size();
        double base = closes.get(n - 1 - days);
        if (base == 0) return null;
        double pct = ((closes.get(n - 1) - base) / base) * 100;
        return Math.round(pct * 100.0) / 100.0;
    }

    // ─────────────────────────────────── BullScore ───────────────────────────

    /**
     * Calcola il BullScore composito (0–100) basato su 7 criteri.
     *
     * <pre>
     * Criterio               Peso
     * Prezzo > SMA200         25pt   trend primario rialzista
     * Prezzo > SMA50          20pt   trend intermedio
     * Prezzo > SMA20          15pt   trend breve
     * RSI tra 50 e 70         15pt   forza senza ipercomprato
     * Momentum30 > 0          10pt   slancio positivo
     * Volatilità < 30%        10pt   stabilità
     * RangePos52W > 50%        5pt   vicino ai massimi annuali
     * ─────────────────────────────
     * Totale                 100pt
     * </pre>
     *
     * @param price       prezzo corrente
     * @param sma20       SMA a 20 giorni
     * @param sma50       SMA a 50 giorni
     * @param sma200      SMA a 200 giorni
     * @param rsi         RSI a 14 periodi
     * @param momentum30  momentum 30 giorni (%)
     * @param volatility  volatilità annualizzata (%)
     * @param rangePos    posizione nel range 52W (0-100)
     * @return score da 0 a 100
     */
    public int calcBullScore(Double price, Double sma20, Double sma50, Double sma200,
                             Integer rsi, Double momentum30, Integer volatility, Integer rangePos) {
        int score = 0;
        if (price != null && sma200 != null && price > sma200)        score += 25;
        if (price != null && sma50  != null && price > sma50)         score += 20;
        if (price != null && sma20  != null && price > sma20)         score += 15;
        if (rsi != null && rsi >= 50 && rsi <= 70)                    score += 15;
        if (momentum30 != null && momentum30 > 0)                     score += 10;
        if (volatility != null && volatility < 30)                    score += 10;
        if (rangePos != null && rangePos > 50)                        score += 5;
        return score;
    }

    // ─────────────────────────────────── Range Position ──────────────────────

    /**
     * Calcola la posizione del prezzo nel range 52 settimane su scala 0-100.
     *
     * @return 0 = al minimo, 100 = al massimo; null se dati insufficienti
     */
    public Integer calcRangePosition(Double price, Double low52w, Double high52w) {
        if (price == null || low52w == null || high52w == null) return null;
        double range = high52w - low52w;
        if (range <= 0) return 50;
        return (int) Math.round(((price - low52w) / range) * 100);
    }

    // ─────────────────────────────────── Helper ──────────────────────────────

    /** Estrae la lista di prezzi di chiusura da una lista di HistoryPoint. */
    public List<Double> extractCloses(List<HistoryPoint> history) {
        if (history == null) return List.of();
        return history.stream()
                .filter(p -> p.close() != null)
                .map(HistoryPoint::close)
                .toList();
    }
}
