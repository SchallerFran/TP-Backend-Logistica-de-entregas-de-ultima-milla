package utnfc.isi.back.logistica.despacho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PlanificacionRequest(
        @NotNull Long centroId,
        @Schema(example = "2026-10-20") @NotNull LocalDate fecha
) {
}
