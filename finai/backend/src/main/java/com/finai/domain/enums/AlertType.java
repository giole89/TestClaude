package com.finai.domain.enums;

/**
 * Tipo di alert sul prezzo.
 *
 * <ul>
 *   <li>{@code ABOVE}       — scatta quando il prezzo supera la soglia</li>
 *   <li>{@code BELOW}       — scatta quando il prezzo scende sotto la soglia</li>
 *   <li>{@code CHANGE_UP}   — scatta quando la variazione % giornaliera supera la soglia</li>
 *   <li>{@code CHANGE_DOWN} — scatta quando la variazione % giornaliera scende sotto (negativa)</li>
 * </ul>
 */
public enum AlertType {
    ABOVE,
    BELOW,
    CHANGE_UP,
    CHANGE_DOWN;

    /** Converte la stringa lowercase usata dal frontend al corrispondente enum. */
    public static AlertType fromString(String value) {
        return switch (value.toLowerCase()) {
            case "above"       -> ABOVE;
            case "below"       -> BELOW;
            case "change_up"   -> CHANGE_UP;
            case "change_down" -> CHANGE_DOWN;
            default -> throw new IllegalArgumentException("AlertType sconosciuto: " + value);
        };
    }

    /** Serializzazione JSON lowercase compatibile con il frontend TypeScript. */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
