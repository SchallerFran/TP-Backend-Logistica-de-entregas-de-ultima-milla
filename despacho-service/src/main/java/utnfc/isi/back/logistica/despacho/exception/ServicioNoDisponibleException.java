package utnfc.isi.back.logistica.despacho.exception;

/**
 * Un componente del que depende la operación (otro microservicio o una API externa) no respondió
 * y no hay forma razonable de degradar (se traduce a HTTP 503).
 */
public class ServicioNoDisponibleException extends RuntimeException {

    private final String servicio;

    public ServicioNoDisponibleException(String servicio, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.servicio = servicio;
    }

    public String getServicio() {
        return servicio;
    }
}
