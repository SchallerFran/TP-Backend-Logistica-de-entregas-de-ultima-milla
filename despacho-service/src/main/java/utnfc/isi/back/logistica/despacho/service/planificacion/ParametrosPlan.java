package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.commons.Coordenada;

import java.time.LocalDate;

public record ParametrosPlan(
        LocalDate fecha,
        Coordenada centro,
        double radioCoberturaKm,
        int tiempoServicioMin
) {
}
