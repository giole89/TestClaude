package com.finai.dto.finance.mortgage;

/**
 * Suggerimento su una possibile fonte a cui attingere per coprire il capitale proprio e le spese
 * accessorie non finanziate dal mutuo, in ordine di priorità/convenienza.
 *
 * @param source  nome sintetico della fonte (es. "Liquidità disponibile", "Fondo pensione")
 * @param message spiegazione testuale del suggerimento
 * @param amount  importo indicativo attingibile da questa fonte per coprire il fabbisogno residuo; null se non quantificabile
 */
public record BudgetAdviceDto(
        String source,
        String message,
        Double amount
) {}
