package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** despacho-service informa el resultado del intento de asignacion. */
public record AsignacionRequest(
        @NotNull Boolean asignado,
        @Size(max = 255) String motivo
) {
}
