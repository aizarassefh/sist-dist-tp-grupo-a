package com.unla.museo.exception;

import com.unla.museo.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfile;

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors.toString(), ex);
    }
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUncaughtException(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                ErrorMessage.INTERNAL_ERROR,
                ex
        );
    }
    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, Exception ex) {
        return buildResponse(status, status.getReasonPhrase(), ex.getMessage(), ex);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message, Exception ex) {
        String stackTrace = null;

        if (!"prod".equalsIgnoreCase(activeProfile)) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            stackTrace = sw.toString();
        }

        ErrorResponse response = new ErrorResponse(
                status,
                error,
                message,
                stackTrace
        );

        return new ResponseEntity<>(response, status);
    }
}
