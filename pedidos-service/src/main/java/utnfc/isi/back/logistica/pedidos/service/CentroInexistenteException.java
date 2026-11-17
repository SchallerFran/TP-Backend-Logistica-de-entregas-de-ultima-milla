package utnfc.isi.back.logistica.pedidos.service;

public class CentroInexistenteException extends RuntimeException {

    public CentroInexistenteException(Long centroId) {
        super("No existe el centro de distribucion " + centroId);
    }
}
