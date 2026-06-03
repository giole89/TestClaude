package com.finai.dto.correlation;

import java.util.List;

/** Matrice di correlazione di Pearson sui ritorni giornalieri. */
public record CorrelationDto(
        /** Lista ordinata dei ticker (labels per righe e colonne della matrice). */
        List<String> labels,
        /** Matrice NxN di correlazioni (valori tra -1 e 1). */
        double[][] matrix,
        /** Interpretazioni testuali delle coppie più significative. */
        List<String> interpretations
) {}
