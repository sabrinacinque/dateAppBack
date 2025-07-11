package com.app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
 
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
 
/**
 * Gestore globale delle eccezioni per l'applicazione.
 * Intercetta e gestisce le eccezioni in modo centralizzato.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
 
    /**
     * Gestisce le eccezioni di autenticazione
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
 
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Unauthorized");
        response.put("message", "Credenziali non valide");
        response.put("path", request.getDescription(false));
 
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
 
    /**
     * Gestisce le eccezioni di accesso negato (403)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
 
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Forbidden");
        response.put("message", "Accesso negato - Privilegi insufficienti");
        response.put("path", request.getDescription(false));
 
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
 
    /**
     * Gestisce le eccezioni di validazione dei dati
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
 
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
 
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
 
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", "Errori di validazione");
        response.put("errors", errors);
 
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
 
    /**
     * Gestisce le eccezioni generiche
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
 
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "Si è verificato un errore interno del server");
        response.put("path", request.getDescription(false));
 
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Gestisce le eccezioni degli Enum tipoAbbonamento
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleEnumError(HttpMessageNotReadableException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("TipoAbbonamento")) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Tipo di abbonamento non valido. Valori ammessi: GOLD, PLATINUM, PREMIUM.");
        }
        else if (ex.getMessage() != null && ex.getMessage().contains("Genere")) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Tipo di genere non valido. Valori ammessi: FEMMINA, MASCHIO.");
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body("Richiesta non valida: " + ex.getMessage());
    }    
    
    
    /**
     * Gestisce le eccezioni dei limiti di LIKE e SUPER_LIKE giornalieri
     */
    @ExceptionHandler(LimitReachedException.class)
    public ResponseEntity <Map<String, Object>> handleLimitException(
    		LimitReachedException ex, WebRequest request) {
    	
    	Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        response.put("error", "Limit reached exception");
        response.put("message", ex.getMessage());
        response.put("path", request.getDescription(false));
 
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }
    
}