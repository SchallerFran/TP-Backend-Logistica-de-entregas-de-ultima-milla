package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.model.EstadoNotificacion;
import utnfc.isi.back.logistica.despacho.model.TipoNotificacion;

import java.time.LocalDateTime;

public record NotificacionResponse(
        Long id,
        TipoNotificacion tipo,
        Long pedidoId,
        Long rutaId,
        Boolean entregado,
        String motivo,
        EstadoNotificacion estado,
        int intentos,
        String ultimoError,
        LocalDateTime creadaEn,
        LocalDateTime enviadaEn
) {
}
