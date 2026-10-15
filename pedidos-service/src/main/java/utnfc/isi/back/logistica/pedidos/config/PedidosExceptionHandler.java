package utnfc.isi.back.logistica.pedidos.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import utnfc.isi.back.logistica.commons.ApiError;
import utnfc.isi.back.logistica.pedidos.service.AccesoDenegadoException;
import utnfc.isi.back.logistica.pedidos.service.CentroInexistenteException;
import utnfc.isi.back.logistica.pedidos.service.ConflictoException;
import utnfc.isi.back.logistica.pedidos.service.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.pedidos.service.ServicioNoDisponibleException;

/**
 * Completa al GlobalExceptionHandler del JAR comun (que cubre validacion 400,
 * IllegalArgumentException 422, RestClientException 503, AccessDenied 403,
 * NoResourceFound 404 y generico 500) con las excepciones propias del servicio.
 *
 * Tiene la precedencia mas alta para ganarle al advice comun, y NO declara un
 * handler generico: todo lo que no sea propio sigue cayendo en commons.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PedidosExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(RecursoNoEncontradoException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ApiError> handleAccesoDenegado(AccesoDenegadoException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> handleConflicto(ConflictoException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(CentroInexistenteException.class)
    public ResponseEntity<ApiError> handleCentroInexistente(CentroInexistenteException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<ApiError> handleServicioNoDisponible(ServicioNoDisponibleException ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
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
