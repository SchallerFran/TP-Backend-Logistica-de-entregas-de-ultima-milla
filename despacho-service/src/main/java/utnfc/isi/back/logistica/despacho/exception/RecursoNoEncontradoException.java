package utnfc.isi.back.logistica.despacho.exception;

/** El recurso pedido no existe (se traduce a HTTP 404 en cada servicio). */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException de(String recurso, Object id) {
        return new RecursoNoEncontradoException(recurso + " no encontrado: " + id);
    }
}
