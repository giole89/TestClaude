package com.finai.dto.finance.mortgage;

/**
 * Idoneità del fondo pensione complementare come possibile fonte per coprire capitale proprio
 * e spese accessorie dell'acquisto, secondo le regole italiane sull'anticipazione (D.Lgs. 252/2005).
 *
 * @param yearsEnrolled           anni di iscrizione dichiarati
 * @param eligibleForHomePurchase true se sono maturati almeno 8 anni di iscrizione: solo da questo momento
 *                                è ammessa l'anticipazione fino al 75% del montante per l'acquisto della
 *                                prima casa (per sé o per i figli); per la seconda casa non è mai ammessa
 * @param yearsUntilEligible      anni mancanti al raggiungimento della soglia degli 8 anni (0 se già raggiunta)
 * @param maxAnticipationPct      quota massima anticipabile in percentuale (75%) se idoneo; null altrimenti
 * @param estimatedMaxAnticipation stima dell'importo massimo anticipabile (montante × 75%), solo se idoneo
 *                                e se è stato dichiarato un montante; null altrimenti
 * @param note                    spiegazione testuale della situazione e delle regole applicabili
 */
public record PensionFundAdviceDto(
        int yearsEnrolled,
        boolean eligibleForHomePurchase,
        int yearsUntilEligible,
        Double maxAnticipationPct,
        Double estimatedMaxAnticipation,
        String note
) {}
