package utnfc.isi.back.logistica.despacho.client;

public class PedidosNoDisponibleException extends RuntimeException {

    public PedidosNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
