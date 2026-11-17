package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.time.LocalTime;

public record RepartidorPlan(
        Long id,
        String nombre,
        LocalTime turnoInicio,
        LocalTime turnoFin
) {
}
