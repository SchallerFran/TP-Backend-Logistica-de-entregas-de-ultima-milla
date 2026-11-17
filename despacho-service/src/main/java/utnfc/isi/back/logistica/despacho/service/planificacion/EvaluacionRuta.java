package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.time.LocalDateTime;
import java.util.List;

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
