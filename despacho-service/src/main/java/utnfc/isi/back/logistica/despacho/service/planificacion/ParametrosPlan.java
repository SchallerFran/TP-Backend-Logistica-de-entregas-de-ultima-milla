package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.geo.Coordenada;

import java.time.LocalDate;

/** Contexto fijo de una planificación: qué día, desde qué centro y con qué tiempo de servicio. */
public record ParametrosPlan(
        LocalDate fecha,
        Coordenada centro,
        double radioCoberturaKm,
        int tiempoServicioMin
) {
}
