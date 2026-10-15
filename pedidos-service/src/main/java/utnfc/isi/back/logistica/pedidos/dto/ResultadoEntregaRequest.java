package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** despacho-service informa el resultado del intento de entrega. */
public record ResultadoEntregaRequest(
        @NotNull Boolean entregado,
        @Size(max = 255) String motivo
) {
}
