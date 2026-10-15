package utnfc.isi.back.logistica.commons;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejo uniforme de errores. Se registra via component-scan en cada servicio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Fallo de validacion de @Valid -> 400. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(MethodArgumentNotValidException ex, WebRequest request) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describir)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, mensaje.isBlank() ? "Datos invalidos" : mensaje, request);
    }

    /** Argumento o regla de negocio invalida -> 422. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleNegocio(IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    /** Servicio externo (otro microservicio o Google Maps) no disponible -> 503. */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleServicioExterno(RestClientException ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Servicio externo no disponible: " + ex.getMessage(), request);
    }

    /** Rol insuficiente (@PreAuthorize) -> 403. Sin este handler, el handler
     *  generico lo convertiria en 500. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acceso denegado", request);
    }

    /** Ruta inexistente -> 404 (idem: evita que caiga en el handler generico). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(NoResourceFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso no encontrado", request);
    }

    /** Cualquier otro error -> 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenerico(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno: " + ex.getMessage(), request);
    }

    private String describir(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String mensaje, WebRequest request) {
        ApiError body = ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getDescription(false).replaceFirst("^uri=", ""));
        return ResponseEntity.status(status).body(body);
    }
}
