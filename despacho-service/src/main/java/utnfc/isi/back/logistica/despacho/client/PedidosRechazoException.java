package utnfc.isi.back.logistica.despacho.client;

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
