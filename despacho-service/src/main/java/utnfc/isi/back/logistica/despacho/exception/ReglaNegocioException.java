package utnfc.isi.back.logistica.despacho.exception;

/**
 * Se violó una regla de negocio o el recurso está en un estado que no admite la operación
 * (se traduce a HTTP 409 Conflict). El código permite que el cliente distinga casos sin parsear texto.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
