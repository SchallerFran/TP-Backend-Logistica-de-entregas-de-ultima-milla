package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;

import java.time.LocalDateTime;

/** Parada planificada: el pedido, su posición y la hora estimada de entrega. */
public record ParadaPlan(
        PedidoPendienteDTO pedido,
        int orden,
        LocalDateTime eta
) {
}
