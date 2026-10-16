package utnfc.isi.back.logistica.despacho.service.planificacion;

import java.util.List;

public record ResultadoPlanificacion(
        List<RutaPlan> rutas,
        List<NoAsignado> noAsignados
) {
}
