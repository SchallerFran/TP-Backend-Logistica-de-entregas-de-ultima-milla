package utnfc.isi.back.logistica.pedidos.service;

/** despacho-service no conoce el centro indicado. Se traduce a 422. */
public class CentroInexistenteException extends RuntimeException {

    public CentroInexistenteException(Long centroId) {
        super("No existe el centro de distribucion " + centroId);
    }
}
