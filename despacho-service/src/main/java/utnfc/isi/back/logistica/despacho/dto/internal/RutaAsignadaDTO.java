package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Pedidos incluidos en una ruta. pedidos-service usa rutaId sólo para el detalle del evento. */
public record RutaAsignadaDTO(
        @NotNull Long rutaId,
        @NotEmpty List<Long> pedidoIds
) {
}
