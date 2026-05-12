package com.finai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestore centralizzato degli errori.
 * Normalizza tutte le eccezioni in un JSON uniforme:
 * <pre>
 * {
 *   "error": "messaggio",
 *   "status": 404,
 *   "timestamp": "2024-..."
 * }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Errori di dominio con codice HTTP esplicito. */
    @ExceptionHandler(FinaiException.class)
    public ResponseEntity<Map<String, Object>> handleFinai(FinaiException ex) {
        log.warn("FinaiException: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(errorBody(ex.getMessage(), ex.getStatusCode()));
    }

    /** Errori di validazione Bean Validation (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validazione fallita: {}", details);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody("Errore di validazione: " + details, 400));
    }

    /** Errori HTTP da WebClient (Yahoo Finance, NASDAQ, Anthropic). */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClient(WebClientResponseException ex) {
        log.warn("Errore WebClient {}: {}", ex.getStatusCode(), ex.getMessage());
        int status = ex.getStatusCode().value();
        String msg = status == 404 ? "Ticker o risorsa non trovata" :
                     status == 429 ? "Troppe richieste all'API esterna" :
                                     "Errore API esterna: " + ex.getMessage();
        return ResponseEntity.status(status).body(errorBody(msg, status));
    }

    /** Fallback per qualsiasi altro errore non gestito. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("Errore non gestito", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Errore interno del server", 500));
    }

    private Map<String, Object> errorBody(String message, int status) {
        return Map.of(
                "error",     message,
                "status",    status,
                "timestamp", Instant.now().toString()
        );
    }
}
