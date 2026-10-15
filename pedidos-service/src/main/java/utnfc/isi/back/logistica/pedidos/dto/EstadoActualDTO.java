package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;

/** Respuesta de los endpoints internos: estado resultante de la operacion. */
public record EstadoActualDTO(
        Long pedidoId,
        EstadoPedido estado,
        int intentos
) {
    public static EstadoActualDTO from(Pedido pedido) {
        return new EstadoActualDTO(pedido.getId(), pedido.getEstado(), pedido.getIntentos());
    }
}
