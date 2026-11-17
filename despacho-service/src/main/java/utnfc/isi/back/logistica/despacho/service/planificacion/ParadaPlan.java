package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;

import java.time.LocalDateTime;

public record ParadaPlan(
        PedidoPendienteDTO pedido,
        int orden,
        LocalDateTime eta
) {
}
