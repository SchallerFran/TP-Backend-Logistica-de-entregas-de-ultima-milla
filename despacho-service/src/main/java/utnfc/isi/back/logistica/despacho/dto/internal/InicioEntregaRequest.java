package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotNull;

public record InicioEntregaRequest(
        @NotNull Long rutaId
) {
}
