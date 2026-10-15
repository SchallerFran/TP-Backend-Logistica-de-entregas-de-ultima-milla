package utnfc.isi.back.logistica.pedidos.service;

/** El recurso pedido no existe. Se traduce a 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException pedido(Long id) {
        return new RecursoNoEncontradoException("No existe el pedido " + id);
    }

    public static RecursoNoEncontradoException cliente(Long id) {
        return new RecursoNoEncontradoException("No existe el cliente " + id);
    }

    public static RecursoNoEncontradoException perfil() {
        return new RecursoNoEncontradoException("El usuario autenticado todavia no tiene perfil de cliente");
    }
}
