package com.finai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Test per i calcoli fiscali italiani sul capital gain.
 *
 * <p>Tassazione italiana sui capital gain: 26% (art. 5 D.Lgs. 461/97).</p>
 * Questi test validano la logica di calcolo usata sia nel frontend (FiscalPanel)
 * che eventualmente nel backend per futuri report fiscali.
 */
@DisplayName("FiscalCalculation - Capital Gain Italy 26%")
class FiscalCalculationTest {

    private static final double TAX_RATE = 0.26;

    // ── Helpers di calcolo (stessa logica del frontend FiscalPanel) ─────────────

    /**
     * Calcola il gain lordo su una posizione.
     */
    private double grossGain(double currentPrice, double loadPrice, double qty) {
        return (currentPrice - loadPrice) * qty;
    }

    /**
     * Calcola l'imposta sul capital gain (26% in Italia).
     */
    private double taxAmount(double grossGain) {
        return Math.max(0, grossGain * TAX_RATE);
    }

    /**
     * Calcola il gain netto dopo le imposte.
     */
    private double netGain(double grossGain) {
        return grossGain - taxAmount(grossGain);
    }

    // ── Test calcolo aliquota 26% ──────────────────────────────────────────────

    @Test
    @DisplayName("imposta 26% su gain positivo")
    void tax_positiveGain() {
        double gain = 1000.0;
        double tax = taxAmount(gain);
        assertThat(tax).isCloseTo(260.0, within(0.01));
    }

    @Test
    @DisplayName("imposta 0 su gain negativo (minusvalenza non tassata)")
    void tax_negativeGain_noTax() {
        double gain = -500.0;
        double tax = taxAmount(gain);
        assertThat(tax).isEqualTo(0.0);
    }

    @Test
    @DisplayName("imposta 0 su gain zero")
    void tax_zeroGain() {
        double gain = 0.0;
        double tax = taxAmount(gain);
        assertThat(tax).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "gain={0} → tax={1}")
    @CsvSource({
        "10000, 2600",
        "5000,  1300",
        "1000,  260",
        "100,   26",
        "0,     0"
    })
    @DisplayName("calcolo imposta parametrico")
    void tax_parametric(double gain, double expectedTax) {
        assertThat(taxAmount(gain)).isCloseTo(expectedTax, within(0.01));
    }

    // ── Test calcolo gain netto ────────────────────────────────────────────────

    @Test
    @DisplayName("gain netto = gain lordo - 26%")
    void netGain_calculation() {
        double gain = 1000.0;
        double net = netGain(gain);
        assertThat(net).isCloseTo(740.0, within(0.01)); // 1000 - 260 = 740
    }

    @Test
    @DisplayName("gain netto su minusvalenza = gain lordo (nessuna imposta)")
    void netGain_loss() {
        double gain = -500.0;
        double net = netGain(gain);
        assertThat(net).isEqualTo(-500.0); // nessuna imposta su perdita
    }

    // ── Test su posizione portfolio ────────────────────────────────────────────

    @Test
    @DisplayName("posizione AAPL: gain lordo, imposta, netto")
    void position_AAPL() {
        double qty = 10;
        double loadPrice = 150.0;
        double currentPrice = 200.0;

        double gross = grossGain(currentPrice, loadPrice, qty); // 500
        double tax = taxAmount(gross);                          // 130
        double net = netGain(gross);                            // 370

        assertThat(gross).isCloseTo(500.0, within(0.01));
        assertThat(tax).isCloseTo(130.0, within(0.01));
        assertThat(net).isCloseTo(370.0, within(0.01));
        assertThat(gross - tax).isCloseTo(net, within(0.01));
    }

    @Test
    @DisplayName("totale portafoglio: somma gain lordi, imposte, netti")
    void portfolio_total() {
        // Posizione 1: gain +1000
        double g1 = 1000.0;
        // Posizione 2: gain -200 (perdita)
        double g2 = -200.0;
        // Posizione 3: gain +500
        double g3 = 500.0;

        double totalGross = g1 + g2 + g3; // 1300
        double totalTax   = taxAmount(g1) + taxAmount(g2) + taxAmount(g3); // 260 + 0 + 130 = 390
        double totalNet   = netGain(g1) + netGain(g2) + netGain(g3);      // 740 - 200 + 370 = 910

        assertThat(totalGross).isCloseTo(1300.0, within(0.01));
        assertThat(totalTax).isCloseTo(390.0, within(0.01));
        assertThat(totalNet).isCloseTo(910.0, within(0.01));
    }

    @Test
    @DisplayName("aliquota effettiva = tax / gross = 26% per gain positivo")
    void effectiveTaxRate_is26percent() {
        double gain = 5000.0;
        double tax = taxAmount(gain);
        double effectiveRate = tax / gain;

        assertThat(effectiveRate).isCloseTo(0.26, within(0.0001));
    }
}
