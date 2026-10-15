package utnfc.isi.back.logistica.pedidos.service;

/** despacho-service no respondio (timeout, conexion o 5xx). Se traduce a 503. */
public class ServicioNoDisponibleException extends RuntimeException {

    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public ServicioNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
