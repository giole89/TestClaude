package com.finai.dto.finance.mortgage;

/**
 * Detrazioni fiscali IRPEF potenzialmente spettanti in dichiarazione dei redditi per l'acquisto
 * della prima casa (art. 15 TUIR): interessi passivi del mutuo ipotecario e spese di intermediazione
 * immobiliare (agenzia). Non spettano per la seconda casa.
 *
 * @param eligible                true se il tipo di acquisto è prima casa (le detrazioni non spettano per la seconda casa)
 * @param interestDeductionRatePct percentuale di detrazione sugli interessi passivi del mutuo (19%)
 * @param maxDeductibleInterestPerYear importo massimo annuo di interessi su cui si applica la detrazione (4.000€)
 * @param estimatedFirstYearInterest quota interessi stimata nel primo anno di mutuo (dal piano di ammortamento):
 *                                 la base su cui è calcolata la stima della detrazione, destinata a ridursi negli anni
 * @param estimatedAnnualInterestDeduction detrazione stimata nel primo anno: 19% del minore tra interessi del primo anno e 4.000€
 * @param agencyFeeDeductionRatePct percentuale di detrazione sulle spese di intermediazione immobiliare (19%)
 * @param maxDeductibleAgencyFee   importo massimo di spesa di agenzia su cui si applica la detrazione (1.000€, una tantum)
 * @param estimatedAgencyFeeDeduction detrazione una tantum stimata: 19% del minore tra spese di agenzia e 1.000€
 * @param note                     spiegazione, condizioni (capienza IRPEF, ripartizione tra cointestatari) e consigli pratici
 */
public record TaxDeductionAdviceDto(
        boolean eligible,
        Double interestDeductionRatePct,
        Double maxDeductibleInterestPerYear,
        Double estimatedFirstYearInterest,
        Double estimatedAnnualInterestDeduction,
        Double agencyFeeDeductionRatePct,
        Double maxDeductibleAgencyFee,
        Double estimatedAgencyFeeDeduction,
        String note
) {}
