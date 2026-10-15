package utnfc.isi.back.logistica.pedidos.dto;

import utnfc.isi.back.logistica.pedidos.model.CambioEstado;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;

import java.time.LocalDateTime;

public record CambioEstadoDTO(
        Long id,
        EstadoPedido estadoAnterior,
        EstadoPedido estadoNuevo,
        String motivo,
        String realizadoPor,
        LocalDateTime fecha
) {
    public static CambioEstadoDTO from(CambioEstado cambio) {
        return new CambioEstadoDTO(
                cambio.getId(),
                cambio.getEstadoAnterior(),
                cambio.getEstadoNuevo(),
                cambio.getMotivo(),
                cambio.getRealizadoPor(),
                cambio.getFecha());
    }
}
