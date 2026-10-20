package utnfc.isi.back.logistica.despacho.client;

/** pedidos-service no respondió (red, timeout, 5xx, token). Es un error transitorio: se puede reintentar. */
public class PedidosNoDisponibleException extends RuntimeException {

    public PedidosNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
