package utnfc.isi.back.logistica.pedidos.service;

/** El usuario autenticado quiere operar sobre el recurso de otro cliente. Se traduce a 403. */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }

    public static AccesoDenegadoException pedidoAjeno(Long id) {
        return new AccesoDenegadoException("El pedido " + id + " pertenece a otro cliente");
    }
}
