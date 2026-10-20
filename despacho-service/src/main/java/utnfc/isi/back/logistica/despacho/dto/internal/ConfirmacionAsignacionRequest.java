package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Resultado de una planificación que despacho le pide confirmar a pedidos en UNA sola llamada,
 * para que pedidos lo aplique en una única transacción:
 * <ul>
 *   <li>cada pedido de {@code rutas} pasa RECIBIDO → ASIGNADO (idempotente si ya está ASIGNADO);</li>
 *   <li>cada pedido de {@code noAsignables} pasa RECIBIDO → NO_ASIGNABLE con el motivo.</li>
 * </ul>
 * En un reintento de confirmación {@code noAsignables} puede venir vacía.
 */
public record ConfirmacionAsignacionRequest(
        @NotNull Long centroId,
        @NotNull LocalDate fecha,
        @NotNull @Valid List<RutaAsignadaDTO> rutas,
        @NotNull @Valid List<PedidoNoAsignableDTO> noAsignables
) {
}
