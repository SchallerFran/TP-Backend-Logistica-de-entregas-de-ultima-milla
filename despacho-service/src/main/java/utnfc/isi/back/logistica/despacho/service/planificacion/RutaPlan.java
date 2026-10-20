package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.util.List;

/** Ruta resultante de la planificación, todavía no persistida. */
public record RutaPlan(
        VehiculoPlan vehiculo,
        RepartidorPlan repartidor,
        List<ParadaPlan> paradas,
        double kmTotal,
        int duracionMin,
        double costoTotal,
        boolean estimada
) {
}
