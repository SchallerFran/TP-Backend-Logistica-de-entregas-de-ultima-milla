package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import utnfc.isi.back.logistica.commons.EstadoPedido;
import utnfc.isi.back.logistica.commons.Prioridad;

public record PedidoDTO(
        Long id,
        @NotBlank String descripcion,
        EstadoPedido estado,
        @NotNull Prioridad prioridad
) {
}
