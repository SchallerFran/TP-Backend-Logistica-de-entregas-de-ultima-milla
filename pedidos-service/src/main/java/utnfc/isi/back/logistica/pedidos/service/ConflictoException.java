package utnfc.isi.back.logistica.pedidos.service;

/** El recurso ya existe o choca con otro. Se traduce a 409. */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
