package utnfc.isi.back.logistica.despacho.dto;

import jakarta.validation.constraints.NotNull;
import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.model.EstadoEnvio;

public record EnvioDTO(
        Long id,
        @NotNull Long pedidoId,
        String repartidorId,
        EstadoEnvio estado,
        Coordenada destino
) {
}
