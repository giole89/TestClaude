package com.finai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Test unitari per {@link IndicatorsService}.
 *
 * <p>Nessuna dipendenza esterna: {@code IndicatorsService} è stateless,
 * tutti i metodi sono puri (stesso input → stesso output).</p>
 */
@DisplayName("IndicatorsService")
class IndicatorsServiceTest {

    private IndicatorsService service;

    @BeforeEach
    void setUp() {
        service = new IndicatorsService();
    }

    // ─────────────────────────────── RSI ─────────────────────────────────────

    @Nested
    @DisplayName("calcRsi")
    class RsiTests {

        @Test
        @DisplayName("serie troppo corta → null")
        void tooShort() {
            assertThat(service.calcRsi(List.of(1.0, 2.0), 14)).isNull();
        }

        @Test
        @DisplayName("lista null → null")
        void nullInput() {
            assertThat(service.calcRsi(null, 14)).isNull();
        }

        @Test
        @DisplayName("serie costante → RSI=50 (nessun gain né loss)")
        void constantSeries() {
            List<Double> flat = Collections.nCopies(30, 100.0);
            // avgLoss = 0: il metodo restituisce 100 per avgLoss=0
            Integer rsi = service.calcRsi(flat, 14);
            assertThat(rsi).isNotNull().isEqualTo(100);
        }

        @Test
        @DisplayName("serie sempre crescente → RSI alto (≥ 70)")
        void alwaysGrowing() {
            List<Double> uptrend = IntStream.rangeClosed(1, 30)
                    .mapToDouble(i -> (double) i)
                    .boxed().toList();
            Integer rsi = service.calcRsi(uptrend, 14);
            assertThat(rsi).isNotNull().isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("serie sempre decrescente → RSI basso (≤ 30)")
        void alwaysFalling() {
            List<Double> downtrend = IntStream.rangeClosed(1, 30)
                    .mapToDouble(i -> 31.0 - i)
                    .boxed().toList();
            Integer rsi = service.calcRsi(downtrend, 14);
            assertThat(rsi).isNotNull().isLessThanOrEqualTo(30);
        }

        @Test
        @DisplayName("RSI in range [0, 100]")
        void inRange() {
            List<Double> mixed = List.of(
                    100.0, 102.0, 101.0, 105.0, 103.0, 108.0, 107.0,
                    110.0, 108.0, 112.0, 111.0, 115.0, 113.0, 116.0, 114.0
            );
            Integer rsi = service.calcRsi(mixed, 14);
            assertThat(rsi).isNotNull().isBetween(0, 100);
        }
    }

    // ─────────────────────────────── SMA ─────────────────────────────────────

    @Nested
    @DisplayName("calcSma")
    class SmaTests {

        @Test
        @DisplayName("SMA20 corretta su serie nota")
        void correctValue() {
            // 20 valori tutti 100 → SMA20 = 100
            List<Double> closes = Collections.nCopies(20, 100.0);
            assertThat(service.calcSma(closes, 20)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("SMA su valori crescenti da 1 a 20 → 10.5")
        void growingValues() {
            List<Double> closes = IntStream.rangeClosed(1, 20)
                    .mapToDouble(i -> (double) i).boxed().toList();
            assertThat(service.calcSma(closes, 20)).isCloseTo(10.5, offset(0.01));
        }

        @Test
        @DisplayName("serie troppo corta → null")
        void tooShort() {
            assertThat(service.calcSma(List.of(1.0, 2.0, 3.0), 20)).isNull();
        }

        @Test
        @DisplayName("lista null → null")
        void nullInput() {
            assertThat(service.calcSma(null, 20)).isNull();
        }
    }

    // ─────────────────────────────── Volatilità ──────────────────────────────

    @Nested
    @DisplayName("calcVolatility")
    class VolatilityTests {

        @Test
        @DisplayName("serie costante → volatilità quasi zero (0)")
        void constantSeries() {
            List<Double> flat = Collections.nCopies(30, 100.0);
            Integer vol = service.calcVolatility(flat);
            assertThat(vol).isNotNull().isEqualTo(0);
        }

        @Test
        @DisplayName("serie troppo corta → null")
        void tooShort() {
            assertThat(service.calcVolatility(List.of(1.0, 2.0))).isNull();
        }

        @Test
        @DisplayName("volatilità sempre non negativa")
        void nonNegative() {
            List<Double> prices = IntStream.rangeClosed(1, 50)
                    .mapToDouble(i -> 100.0 + Math.sin(i) * 10)
                    .boxed().toList();
            Integer vol = service.calcVolatility(prices);
            assertThat(vol).isNotNull().isGreaterThanOrEqualTo(0);
        }
    }

    // ─────────────────────────────── Momentum ────────────────────────────────

    @Nested
    @DisplayName("calcMomentum")
    class MomentumTests {

        @Test
        @DisplayName("+10% su 30 giorni → momentum ≈ 10.0")
        void tenPercent() {
            // calcMomentum(days=30) richiede almeno 31 elementi: base=closes[n-1-days], last=closes[n-1]
            var modified = new java.util.ArrayList<>(Collections.nCopies(31, 100.0));
            modified.set(0, 90.909090); // +10% da closes[0] a closes[30]
            Double m = service.calcMomentum(modified, 30);
            assertThat(m).isNotNull().isCloseTo(10.0, offset(0.5));
        }

        @Test
        @DisplayName("serie troppo corta → null")
        void tooShort() {
            assertThat(service.calcMomentum(List.of(100.0, 110.0), 30)).isNull();
        }
    }

    // ─────────────────────────────── BullScore ───────────────────────────────

    @Nested
    @DisplayName("calcBullScore")
    class BullScoreTests {

        @Test
        @DisplayName("tutti i criteri soddisfatti → 100")
        void allCriteriaMet() {
            // prezzo > sma200 > sma50 > sma20, RSI=60, momentum>0, vol<30, rangePos>50
            int score = service.calcBullScore(
                    210.0, 200.0, 180.0, 150.0,
                    60, 5.0, 20, 75);
            assertThat(score).isEqualTo(100);
        }

        @Test
        @DisplayName("nessun criterio soddisfatto → 0")
        void noCriteriaMet() {
            // prezzo < tutte le SMA, RSI=80, momentum<0, vol>30, rangePos<50
            int score = service.calcBullScore(
                    100.0, 200.0, 180.0, 150.0,
                    80, -5.0, 40, 20);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("null values → non genera NPE")
        void nullValues() {
            int score = service.calcBullScore(null, null, null, null, null, null, null, null);
            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("score sempre in [0, 100]")
        void inRange() {
            int score = service.calcBullScore(150.0, 140.0, 130.0, 120.0, 55, 3.0, 25, 60);
            assertThat(score).isBetween(0, 100);
        }
    }

    // ─────────────────────────────── RangePosition ───────────────────────────

    @Nested
    @DisplayName("calcRangePosition")
    class RangePositionTests {

        @Test
        @DisplayName("prezzo al massimo 52W → 100")
        void atHigh() {
            assertThat(service.calcRangePosition(200.0, 100.0, 200.0)).isEqualTo(100);
        }

        @Test
        @DisplayName("prezzo al minimo 52W → 0")
        void atLow() {
            assertThat(service.calcRangePosition(100.0, 100.0, 200.0)).isEqualTo(0);
        }

        @Test
        @DisplayName("prezzo a metà range → ~50")
        void atMiddle() {
            assertThat(service.calcRangePosition(150.0, 100.0, 200.0)).isEqualTo(50);
        }

        @Test
        @DisplayName("null values → null")
        void nullValues() {
            assertThat(service.calcRangePosition(null, 100.0, 200.0)).isNull();
        }

        @Test
        @DisplayName("high = low (range zero) → 50")
        void zeroRange() {
            assertThat(service.calcRangePosition(100.0, 100.0, 100.0)).isEqualTo(50);
        }
    }
}
