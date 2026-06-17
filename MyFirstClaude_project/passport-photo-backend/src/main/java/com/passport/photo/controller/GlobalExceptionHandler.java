package com.passport.photo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * Catch-all REST exception handler that prevents internal error details from
 * leaking to API clients.
 *
 * <p>{@link IllegalArgumentException} maps to HTTP 400 (bad request) and
 * includes the exception message verbatim — this is intentional because
 * {@code IllegalArgumentException} is used throughout the application for
 * user-input validation errors. All other exceptions map to HTTP 500 with a
 * generic message; only the exception type and message are written to the log.</p>
 *
 * <p>Note: {@link PhotoController} already handles {@code IllegalArgumentException}
 * and {@code IOException} internally with its own try/catch blocks, so those
 * handlers will always fire first. This handler is the safety net for any
 * exception that escapes the controller methods — including future endpoints.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link IllegalArgumentException} — typically a user-input validation
     * error. Returns HTTP 400 with the exception message.
     *
     * @param ex the exception
     * @return 400 Bad Request with {@code {"error": "Invalid request: <message>"}}
     */
    private static String safeMsg(Exception ex) {
        String msg = ex.getMessage();
        return msg == null ? "" : msg.replaceAll("[\r\n]", " ");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: " + ex.getMessage()));
    }

    /**
     * Handles Spring MVC binding/validation errors that must remain 400 rather than
     * falling through to the generic 500 handler.
     *
     * <p>Covers:
     * <ul>
     *   <li>{@link MethodArgumentNotValidException} — {@code @Valid} bean validation failures</li>
     *   <li>{@link HttpMessageNotReadableException} — malformed request body (bad JSON etc.)</li>
     *   <li>{@link MissingServletRequestParameterException} — required query/form param absent</li>
     *   <li>{@link MethodArgumentTypeMismatchException} — type conversion failure for a param</li>
     *   <li>{@link MaxUploadSizeExceededException} — multipart file too large (413)</li>
     * </ul>
     * </p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: validation failed"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: malformed or unreadable request body"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Required request parameter is missing."));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid request: parameter type mismatch"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "Invalid request: uploaded file exceeds the maximum allowed size"));
    }

    /**
     * Catch-all handler for any exception not handled elsewhere. Returns HTTP 500
     * without exposing internal paths, stack traces, or implementation details.
     *
     * @param ex the exception
     * @return 500 Internal Server Error with {@code {"error": "An unexpected error occurred"}}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("{}: {}", ex.getClass().getSimpleName(), safeMsg(ex));
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
    }
}
