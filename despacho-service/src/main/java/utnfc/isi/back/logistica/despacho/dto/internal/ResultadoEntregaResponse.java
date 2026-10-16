package utnfc.isi.back.logistica.despacho.dto.internal;

import utnfc.isi.back.logistica.commons.EstadoPedido;

/** Estado en que quedó el pedido luego de aplicar el resultado. */
public record ResultadoEntregaResponse(
        Long pedidoId,
        EstadoPedido estado,
        int intentos
) {
}
