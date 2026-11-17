package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PedidoNoAsignableDTO(
        @NotNull Long pedidoId,
        @NotBlank String codigo,
        String detalle
) {
}
