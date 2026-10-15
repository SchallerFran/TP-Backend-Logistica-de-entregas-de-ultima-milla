package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.Pedido;

import java.time.LocalDateTime;

/** Datos minimos que despacho-service necesita para asignar. */
public record PedidoPendienteDTO(
        Long id,
        Double latitud,
        Double longitud,
        Double pesoKg,
        Double volumenM3,
        String prioridad,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        boolean requiereFrio
) {
    public static PedidoPendienteDTO from(Pedido pedido) {
        return new PedidoPendienteDTO(
                pedido.getId(),
                pedido.getDestinatario() == null ? null : pedido.getDestinatario().getLatitud(),
                pedido.getDestinatario() == null ? null : pedido.getDestinatario().getLongitud(),
                pedido.getPesoKg(),
                pedido.getVolumenM3(),
                pedido.getPrioridad() == null ? null : pedido.getPrioridad().name(),
                pedido.getVentanaDesde(),
                pedido.getVentanaHasta(),
                pedido.isRequiereFrio());
    }
}
