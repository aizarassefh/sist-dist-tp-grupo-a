package com.unla.museo.comun.errores;

import com.unla.museo.seguridad.exception.UsuarioExistenteException;
import com.unla.museo.seguridad.exception.UsuarioNoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Único punto de traducción de excepciones a respuestas HTTP. Nunca devuelve
 * stack traces ni nombres de clases al cliente: una excepción no controlada
 * se loguea completa acá y al cliente le llega un mensaje genérico.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UsuarioExistenteException.class)
    public ResponseEntity<RespuestaError> handleUsuarioYaExiste(UsuarioExistenteException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<RespuestaError> handleConflicto(ConflictoException ex) {
        return construir(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler({UsuarioNoEncontradoException.class, RecursoInexistenteException.class, com.unla.museo.comun.errores.RecursoInexistenteException.class})
    public ResponseEntity<RespuestaError> handleRecursoNoEncontrado(RuntimeException ex) {
        return construir(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<RespuestaError> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return construir(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespuestaError> handleCredencialesInvalidas(BadCredentialsException ex) {
        return construir(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    // Cubre también AuthorizationDeniedException (subclase que lanza @PreAuthorize
    // al denegar), porque Spring MVC resuelve el @ExceptionHandler más específico
    // que matchee, recorriendo la jerarquía de la excepción.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> handleAccesoDenegado(AccessDeniedException ex) {
        return construir(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta acción", null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> handleArgumentoInvalido(MethodArgumentNotValidException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                detalles.put(error.getField(), error.getDefaultMessage()));
        return construir(HttpStatus.BAD_REQUEST, "La solicitud tiene datos inválidos", detalles);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RespuestaError> handleParametroInvalido(HandlerMethodValidationException ex) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String campo = resultado.getMethodParameter().getParameterName();
            String motivo = resultado.getResolvableErrors().stream()
                    .map(MessageSourceResolvable::getDefaultMessage)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Valor inválido");
            detalles.put(campo != null ? campo : "parámetro", motivo);
        });
        return construir(HttpStatus.BAD_REQUEST, "La solicitud tiene parámetros inválidos", detalles);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no se puede interpretar", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> handleTipoDeParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construir(HttpStatus.BAD_REQUEST,
                "El parámetro '" + ex.getName() + "' tiene un valor o tipo inválido", null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> handleRutaInexistente(NoResourceFoundException ex) {
        return construir(HttpStatus.NOT_FOUND, "El recurso solicitado no existe", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> handleViolacionDeIntegridad(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMessage());
        return construir(HttpStatus.CONFLICT, "La operación viola una restricción de datos existente", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> handleExcepcionNoControlada(Exception ex) {
        log.error("Excepción no controlada", ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, MensajesError.INTERNAL_ERROR, null);
    }

    private ResponseEntity<RespuestaError> construir(HttpStatus status, String mensaje, Map<String, String> detalles) {
        RespuestaError cuerpo = new RespuestaError(status.value(), status.name(), mensaje, detalles);
        return new ResponseEntity<>(cuerpo, status);
    }
}
