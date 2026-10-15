package utnfc.isi.back.logistica.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import utnfc.isi.back.logistica.pedidos.model.PrioridadPedido;

import java.time.LocalDateTime;

public record CrearPedidoRequest(
        @NotNull Long centroId,
        @NotNull @Valid DestinatarioDTO destinatario,
        @NotNull @Positive Double pesoKg,
        @NotNull @Positive Double volumenM3,
        @NotNull PrioridadPedido prioridad,
        @NotNull LocalDateTime ventanaDesde,
        @NotNull LocalDateTime ventanaHasta,
        boolean requiereFrio
) {
}
