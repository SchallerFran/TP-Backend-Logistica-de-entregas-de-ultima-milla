package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.model.PrioridadPedido;

import java.time.LocalDateTime;

public record PedidoDTO(
        Long id,
        Long clienteId,
        DestinatarioDTO destinatario,
        Long centroId,
        Double pesoKg,
        Double volumenM3,
        PrioridadPedido prioridad,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        boolean requiereFrio,
        EstadoPedido estado,
        int intentos,
        LocalDateTime creadoEn
) {
    /** Mapear siempre dentro de la transaccion (open-in-view esta desactivado). */
    public static PedidoDTO from(Pedido pedido) {
        return new PedidoDTO(
                pedido.getId(),
                pedido.getCliente() == null ? null : pedido.getCliente().getId(),
                DestinatarioDTO.from(pedido.getDestinatario()),
                pedido.getCentroId(),
                pedido.getPesoKg(),
                pedido.getVolumenM3(),
                pedido.getPrioridad(),
                pedido.getVentanaDesde(),
                pedido.getVentanaHasta(),
                pedido.isRequiereFrio(),
                pedido.getEstado(),
                pedido.getIntentos(),
                pedido.getCreadoEn());
    }
}
