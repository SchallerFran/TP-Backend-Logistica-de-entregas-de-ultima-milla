package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RutaAsignadaDTO(
        @NotNull Long rutaId,
        @NotEmpty List<Long> pedidoIds
) {
}
