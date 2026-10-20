package utnfc.isi.back.logistica.despacho.client;

/** Google no respondió, respondió con error HTTP o con un status global distinto de OK. */
public class GoogleNoDisponibleException extends RuntimeException {

    public GoogleNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public GoogleNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
