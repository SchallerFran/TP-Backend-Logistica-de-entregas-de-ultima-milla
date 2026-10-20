package utnfc.isi.back.logistica.despacho.dto;

import utnfc.isi.back.logistica.despacho.model.EstadoRuta;

import java.time.LocalDate;
import java.util.List;

public record RutaResponse(
        Long id,
        LocalDate fecha,
        EstadoRuta estado,
        Long centroId,
        Long vehiculoId,
        String patente,
        Long repartidorId,
        String repartidor,
        double kmTotal,
        int duracionMin,
        double costoTotal,
        boolean estimada,
        List<ParadaResponse> paradas
) {
}
