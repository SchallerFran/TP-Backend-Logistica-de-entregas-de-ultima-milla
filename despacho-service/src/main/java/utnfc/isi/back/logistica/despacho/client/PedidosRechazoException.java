package utnfc.isi.back.logistica.despacho.client;

/**
 * pedidos-service respondió, pero rechazó la operación por una regla de negocio (4xx).
 * Reintentar no sirve: hay que revisar el caso.
 */
public class PedidosRechazoException extends RuntimeException {

    private final int status;

    public PedidosRechazoException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
