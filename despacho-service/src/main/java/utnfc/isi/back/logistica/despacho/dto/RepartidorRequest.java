package utnfc.isi.back.logistica.despacho.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record RepartidorRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Schema(description = "Claim 'sub' del usuario del repartidor en Keycloak")
        @NotBlank @Size(max = 64) String keycloakUserId,
        @Schema(type = "string", example = "08:00") @NotNull LocalTime turnoInicio,
        @Schema(type = "string", example = "17:00") @NotNull LocalTime turnoFin,
        Boolean activo,
        @NotNull Long centroId
) {

    @JsonIgnore
    @AssertTrue(message = "turnoFin debe ser posterior a turnoInicio")
    public boolean isTurnoValido() {
        return turnoInicio == null || turnoFin == null || turnoFin.isAfter(turnoInicio);
    }
}
