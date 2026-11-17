package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.util.List;

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
