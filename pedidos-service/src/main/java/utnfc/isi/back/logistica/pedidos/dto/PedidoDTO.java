package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.PrioridadPedido;

public record PedidoDTO(
        Long id,
        @NotBlank String descripcion,
        EstadoPedido estado,
        @NotNull PrioridadPedido prioridad
) {
}
