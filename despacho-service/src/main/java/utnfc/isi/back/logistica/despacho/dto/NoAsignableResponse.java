package utnfc.isi.back.logistica.despacho.dto;

public record NoAsignableResponse(
        Long pedidoId,
        String motivo,
        String detalle
) {
}
