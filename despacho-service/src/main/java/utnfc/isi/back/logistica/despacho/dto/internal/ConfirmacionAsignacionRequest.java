package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record ConfirmacionAsignacionRequest(
        @NotNull Long centroId,
        @NotNull LocalDate fecha,
        @NotNull @Valid List<RutaAsignadaDTO> rutas,
        @NotNull @Valid List<PedidoNoAsignableDTO> noAsignables
) {
}
