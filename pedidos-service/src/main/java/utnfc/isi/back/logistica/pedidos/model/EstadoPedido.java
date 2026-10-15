package utnfc.isi.back.logistica.pedidos.model;

/**
 * Estados del ciclo de vida de un pedido.
 *
 * RECIBIDO y FALLIDO no figuraban en el listado del enunciado pero son
 * necesarios: el pedido nace en RECIBIDO y registrarFallo() pasa por FALLIDO
 * antes de reintentar o devolver.
 */
public enum EstadoPedido {
    RECIBIDO,
    ASIGNADO,
    EN_CAMINO,
    ENTREGADO,
    FALLIDO,
    REPROGRAMADO,
    DEVUELTO,
    CANCELADO
}
