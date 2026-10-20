package utnfc.isi.back.logistica.despacho.dto.internal;

import java.util.List;

/**
 * Respuesta de pedidos a la confirmación. Un pedido puede rechazarse si cambió de estado
 * entre que despacho lo leyó y lo confirmó (por ejemplo, el cliente lo canceló).
 * despacho quita esas paradas y recalcula la ruta.
 */
public record ConfirmacionAsignacionResponse(
        List<Long> confirmados,
        List<PedidoRechazadoDTO> rechazados
) {
}
