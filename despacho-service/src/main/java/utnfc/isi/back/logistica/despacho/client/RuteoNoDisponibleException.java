package utnfc.isi.back.logistica.despacho.client;

public class RuteoNoDisponibleException extends RuntimeException {

    public RuteoNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public RuteoNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
