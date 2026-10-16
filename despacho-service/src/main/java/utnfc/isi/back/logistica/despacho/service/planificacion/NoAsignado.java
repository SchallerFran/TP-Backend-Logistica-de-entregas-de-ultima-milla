package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;

/** Pedido que quedó fuera de la planificación, con el motivo y un detalle legible. */
public record NoAsignado(
        PedidoPendienteDTO pedido,
        MotivoNoAsignable motivo,
        String detalle
) {
}
