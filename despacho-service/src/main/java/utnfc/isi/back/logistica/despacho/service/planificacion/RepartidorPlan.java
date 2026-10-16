package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.time.LocalTime;

/** Vista del repartidor que necesita el planificador. */
public record RepartidorPlan(
        Long id,
        String nombre,
        LocalTime turnoInicio,
        LocalTime turnoFin
) {
}
