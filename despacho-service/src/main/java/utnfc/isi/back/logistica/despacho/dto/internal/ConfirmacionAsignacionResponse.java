package utnfc.isi.back.logistica.despacho.dto.internal;

import java.util.List;

public record ConfirmacionAsignacionResponse(
        List<Long> confirmados,
        List<PedidoRechazadoDTO> rechazados
) {
}
