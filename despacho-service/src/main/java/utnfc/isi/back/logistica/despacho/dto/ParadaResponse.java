package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.model.EstadoParada;

import java.time.LocalDateTime;

public record ParadaResponse(
        Long id,
        int orden,
        Long pedidoId,
        double latitud,
        double longitud,
        double pesoKg,
        double volumenM3,
        LocalDateTime ventanaDesde,
        LocalDateTime ventanaHasta,
        LocalDateTime etaEstimada,
        EstadoParada estado,
        String motivoFallo
) {
}
