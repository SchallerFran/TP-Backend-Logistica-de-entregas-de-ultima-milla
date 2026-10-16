package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.model.EstadoParada;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResultadoParadaRequest(
        @Schema(allowableValues = {"ENTREGADA", "FALLIDA"}) @NotNull EstadoParada resultado,
        @Schema(description = "Obligatorio si resultado = FALLIDA", example = "Destinatario ausente")
        @Size(max = 255) String motivo
) {

    @JsonIgnore
    @AssertTrue(message = "resultado debe ser ENTREGADA o FALLIDA")
    public boolean isResultadoValido() {
        return resultado == null || resultado != EstadoParada.PENDIENTE;
    }

    @JsonIgnore
    @AssertTrue(message = "motivo es obligatorio cuando la entrega es FALLIDA")
    public boolean isMotivoPresenteSiFalla() {
        return resultado != EstadoParada.FALLIDA || (motivo != null && !motivo.isBlank());
    }
}
