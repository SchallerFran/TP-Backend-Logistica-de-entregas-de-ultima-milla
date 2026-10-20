package utnfc.isi.back.logistica.despacho.dto.internal;

import utnfc.isi.back.logistica.commons.EstadoPedido;

/** Pedido que pedidos-service no pudo pasar a ASIGNADO, con su estado actual. */
public record PedidoRechazadoDTO(
        Long pedidoId,
        EstadoPedido estadoActual,
        String motivo
) {
}
