package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido que no pudo incorporarse a ninguna ruta.
 *
 * @param codigo  código estable del motivo (ej. FUERA_DE_COBERTURA, SIN_VEHICULO_REFRIGERADO)
 * @param detalle texto legible para el historial del pedido
 */
public record PedidoNoAsignableDTO(
        @NotNull Long pedidoId,
        @NotBlank String codigo,
        String detalle
) {
}
