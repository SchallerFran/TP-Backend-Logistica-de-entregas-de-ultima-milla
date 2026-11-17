package utnfc.isi.back.logistica.despacho.dto.internal;

public record PedidoRechazadoDTO(
        Long pedidoId,
        String estadoActual,
        String motivo
) {
}
