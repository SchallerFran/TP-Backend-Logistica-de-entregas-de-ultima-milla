package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;

public record NoAsignado(
        PedidoPendienteDTO pedido,
        MotivoNoAsignable motivo,
        String detalle
) {
}
