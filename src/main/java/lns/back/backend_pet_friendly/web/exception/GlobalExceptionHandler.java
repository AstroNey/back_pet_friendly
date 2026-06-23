package lns.back.backend_pet_friendly.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import lns.back.backend_pet_friendly.domain.exception.DuplicateReviewException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    public record ErrorResponse(Instant timestamp, int status, String error, String path) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(new ErrorResponse(Instant.now(), 404, ex.getMessage(), req.getRequestURI()));
    }
    @ExceptionHandler(lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDomainNotFound(lns.back.backend_pet_friendly.domain.exception.ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404).body(new ErrorResponse(Instant.now(), 404, ex.getMessage(), req.getRequestURI()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.status(400).body(new ErrorResponse(Instant.now(), 400, ex.getMessage(), req.getRequestURI()));
    }
    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateReviewException ex, HttpServletRequest req) {
        return ResponseEntity.status(409).body(new ErrorResponse(Instant.now(), 409, ex.getMessage(), req.getRequestURI()));
    }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(HttpServletRequest req) {
        return ResponseEntity.status(403).body(new ErrorResponse(Instant.now(), 403, "Access denied", req.getRequestURI()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(new ErrorResponse(Instant.now(), 400, msg, req.getRequestURI()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(HttpServletRequest req) {
        return ResponseEntity.status(500).body(new ErrorResponse(Instant.now(), 500, "Internal server error", req.getRequestURI()));
    }
}
