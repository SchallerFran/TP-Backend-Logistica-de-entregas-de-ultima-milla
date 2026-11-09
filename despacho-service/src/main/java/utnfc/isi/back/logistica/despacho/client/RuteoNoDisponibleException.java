package utnfc.isi.back.logistica.despacho.client;

/** El servicio de ruteo (OSRM) no respondió, respondió con error HTTP o con un code distinto de Ok. */
public class RuteoNoDisponibleException extends RuntimeException {

    public RuteoNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public RuteoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
