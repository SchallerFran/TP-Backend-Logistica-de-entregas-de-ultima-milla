package utnfc.isi.back.logistica.despacho.dto;

import jakarta.validation.constraints.NotNull;
import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.commons.EstadoPedido;

public record EnvioDTO(
        Long id,
        @NotNull Long pedidoId,
        String repartidorId,
        EstadoPedido estado,
        Coordenada destino
) {
}
