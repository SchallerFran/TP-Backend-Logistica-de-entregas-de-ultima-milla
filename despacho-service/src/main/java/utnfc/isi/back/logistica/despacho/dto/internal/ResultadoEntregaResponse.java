package utnfc.isi.back.logistica.despacho.dto.internal;

/** Estado en que quedó el pedido luego de aplicar el resultado. */
public record ResultadoEntregaResponse(
        Long pedidoId,
        String estado,
        int intentos
) {
}
