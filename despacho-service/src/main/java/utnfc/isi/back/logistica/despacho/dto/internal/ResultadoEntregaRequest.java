package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResultadoEntregaRequest(
        @NotNull Long rutaId,
        boolean entregado,
        @Size(max = 255) String motivo
) {
}
