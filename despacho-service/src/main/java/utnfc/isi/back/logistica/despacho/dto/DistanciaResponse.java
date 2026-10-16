package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.service.planificacion.FuenteDistancia;

public record DistanciaResponse(
        String origen,
        String destino,
        double km,
        int minutos,
        FuenteDistancia fuente
) {
}
