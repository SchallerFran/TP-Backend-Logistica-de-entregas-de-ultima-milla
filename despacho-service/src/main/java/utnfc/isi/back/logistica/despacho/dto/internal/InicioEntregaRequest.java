package utnfc.isi.back.logistica.despacho.dto.internal;

import jakarta.validation.constraints.NotNull;

/** El repartidor inició la ruta que contiene al pedido: ASIGNADO → EN_CAMINO. */
public record InicioEntregaRequest(
        @NotNull Long rutaId
) {
}
