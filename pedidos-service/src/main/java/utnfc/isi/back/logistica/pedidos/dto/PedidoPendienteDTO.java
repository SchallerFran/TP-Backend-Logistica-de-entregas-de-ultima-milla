package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.model.PrioridadPedido;

import java.time.LocalDateTime;

/** Datos minimos que despacho-service necesita para asignar (sin datos personales). */
public record PedidoPendienteDTO(
        Long id,
        Long centroId,
        Double latitud,
        Double longitud,
        Double pesoKg,
        Double volumenM3,
        PrioridadPedido prioridad,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        boolean requiereFrio,
        int intentos
) {
    public static PedidoPendienteDTO from(Pedido pedido) {
        return new PedidoPendienteDTO(
                pedido.getId(),
                pedido.getCentroId(),
                pedido.getDestinatario().getLatitud(),
                pedido.getDestinatario().getLongitud(),
                pedido.getPesoKg(),
                pedido.getVolumenM3(),
                pedido.getPrioridad(),
                pedido.getVentanaDesde(),
                pedido.getVentanaHasta(),
                pedido.isRequiereFrio(),
                pedido.getIntentos());
    }
}
