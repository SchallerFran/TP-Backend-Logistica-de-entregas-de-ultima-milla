package utnfc.isi.back.logistica.despacho.dto.internal;

public record ResultadoEntregaResponse(
        Long pedidoId,
        String estado,
        int intentos
) {
}
