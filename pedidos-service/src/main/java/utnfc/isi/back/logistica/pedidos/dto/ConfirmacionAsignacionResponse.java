package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;

import java.util.List;

public record ConfirmacionAsignacionResponse(
        List<Long> confirmados,
        List<Rechazado> rechazados
) {
    public record Rechazado(Long pedidoId, EstadoPedido estadoActual, String motivo) {
    }
}
