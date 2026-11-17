package utnfc.isi.back.logistica.pedidos.service;

public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public static AccesoDenegadoException pedidoAjeno(Long id) {
        return new AccesoDenegadoException("El pedido " + id + " pertenece a otro cliente");
    }
}
