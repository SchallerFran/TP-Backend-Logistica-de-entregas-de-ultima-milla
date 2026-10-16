package utnfc.isi.back.logistica.despacho.dto;

import jakarta.validation.constraints.*;

public record VehiculoRequest(
        @NotBlank @Size(max = 10) @Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "solo letras, números y espacios")
        String patente,
        @NotNull @Positive Double pesoMaxKg,
        @NotNull @Positive Double volMaxM3,
        @NotNull @Positive Double autonomiaKm,
        @NotNull @PositiveOrZero Double costoPorKm,
        Boolean tieneFrio,
        Boolean disponible,
        @NotNull Long centroId
) {
}
