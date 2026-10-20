package utnfc.isi.back.logistica.pedidos.model;

/**
 * Estados del ciclo de vida de un pedido.
 *
 * RECIBIDO: espera planificacion (alta, reintento o reprogramacion).
 * NO_ASIGNABLE: la planificacion no encontro recurso; el despachante lo
 * reprograma (vuelve a RECIBIDO) o lo cancela.
 * FALLIDO: paso intermedio de registrarFallo() antes de reintentar o devolver.
 */
public enum EstadoPedido {
    RECIBIDO,
    ASIGNADO,
    NO_ASIGNABLE,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO,
    DEVUELTO,
    CANCELADO
}
