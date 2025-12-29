package com.netflix.clone.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.time.Instant;
import java.util.Map;


@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccountDeactivatedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountDeactivated(AccountDeactivatedException ex) {
        log.warn("Account deactivated exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.warn("Email not verified exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<Map<String, Object>> handleEmailSending(EmailSendingException ex) {
        log.warn("Email sending exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidTokenException ex) {
        log.warn("Invalid token exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRole(InvalidRoleException ex) {
        log.warn("Invalid role exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        log.warn("Email already exists exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse("Invalid request");

        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(Map.of("timestamp", Instant.now(), "stats", status.value(), "error", message));
    }

    @ExceptionHandler({AsyncRequestNotUsableException.class, ClientAbortException.class})
    public void handleClientAbort(Exception ex) {
        log.debug("Client aborted request", ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.warn("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = Map.of("timestamp", Instant.now(), "error", message);
        return new ResponseEntity<>(body, status);
    }


}
