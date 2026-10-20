package utnfc.isi.back.logistica.despacho.exception;

import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.exception.ServicioNoDisponibleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiene precedencia sobre el GlobalExceptionHandler de logistica-commons (mismo nombre de clase
 * evitado a proposito para que no choquen los beans) porque maneja las excepciones propias de despacho.
 * Errores en formato ProblemDetail (RFC 7807), como recomienda el Apunte 18.
 * Se agrega la propiedad "codigo" para que el cliente distinga casos sin parsear el texto.
 * <pre>
 * 400 datos inválidos · 403 no es su recurso · 404 no existe · 409 regla de negocio / estado
 * 503 dependencia caída · 500 inesperado (sin detalles internos)
 * </pre>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DespachoExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        e.getBindingResult().getGlobalErrors().forEach(ge -> errores.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "La solicitud tiene datos inválidos");
        pd.setProperty("errores", errores);
        return pd;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class})
    public ProblemDetail malFormada(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "SOLICITUD_MAL_FORMADA", mensajeCorto(e));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException e) {
        return problema(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "CONFLICTO_DE_DATOS",
                "La operación entra en conflicto con datos existentes (registro duplicado o en uso)");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ProblemDetail noDisponible(ServicioNoDisponibleException e) {
        ProblemDetail pd = problema(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", e.getMessage());
        pd.setProperty("servicio", e.getServicio());
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail rutaInexistente(NoResourceFoundException e) {
        return problema(HttpStatus.NOT_FOUND, "ENDPOINT_INEXISTENTE", "No existe el recurso " + e.getResourcePath());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail metodoNoSoportado(HttpRequestMethodNotSupportedException e) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_SOPORTADO", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail inesperado(Exception e) {
        log.error("Error inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error inesperado. Ver logs del servicio.");
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String detalle) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalle);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("codigo", codigo);
        return pd;
    }

    private static String mensajeCorto(Exception e) {
        String m = e.getMessage();
        if (m == null) {
            return "Solicitud mal formada";
        }
        return m.length() > 300 ? m.substring(0, 300) : m;
    }
}
