package utnfc.isi.back.logistica.despacho.dto.internal;

/** Pedido que pedidos-service no pudo pasar a ASIGNADO, con su estado actual. */
public record PedidoRechazadoDTO(
        Long pedidoId,
        String estadoActual,
        String motivo
) {
}
