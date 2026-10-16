package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoRechazadoDTO;
import utnfc.isi.back.logistica.despacho.service.planificacion.FuenteDistancia;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PlanificacionResponse(
        Long centroId,
        LocalDate fecha,
        EstadoPlanificacion estado,
        int pedidosEvaluados,
        List<RutaResponse> rutas,
        List<NoAsignableResponse> noAsignables,
        List<PedidoRechazadoDTO> rechazadosPorPedidos,
        Map<FuenteDistancia, Integer> fuentesDistancia,
        List<String> advertencias
) {
}
