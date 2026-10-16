package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de simular una secuencia de paradas con un vehículo y un repartidor.
 *
 * @param violacion null si es factible; si no, la primera restricción violada (las métricas se informan igual)
 * @param etas      hora estimada de entrega de cada parada, en el orden de la secuencia
 */
public record EvaluacionRuta(
        MotivoNoAsignable violacion,
        double kmTotal,
        int duracionMin,
        List<LocalDateTime> etas,
        LocalDateTime regreso,
        boolean estimada
) {

    public boolean factible() {
        return violacion == null;
    }
}
