package utnfc.isi.back.logistica.despacho.dto;

import jakarta.validation.constraints.*;

public record CentroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 200) String direccion,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitud,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitud,
        @NotNull @Positive Double radioCoberturaKm,
        Boolean activo
) {
}
