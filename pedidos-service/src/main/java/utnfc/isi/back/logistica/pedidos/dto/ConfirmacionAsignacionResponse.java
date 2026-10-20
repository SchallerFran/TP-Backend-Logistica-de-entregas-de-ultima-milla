package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;

import java.util.List;

/**
 * confirmados: pedidos que quedaron ASIGNADO.
 * rechazados: pedidos que cambiaron de estado mientras se planificaba (ej. cancelados);
 * despacho-service los quita de sus rutas.
 */
public record ConfirmacionAsignacionResponse(
        List<Long> confirmados,
        List<Rechazado> rechazados
) {
    public record Rechazado(Long pedidoId, EstadoPedido estadoActual, String motivo) {
    }
}
