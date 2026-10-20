package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;
import utnfc.isi.back.logistica.commons.Coordenada;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simula una ruta (centro → paradas en orden → centro) y verifica TODAS las restricciones del dominio
 * al mismo tiempo:
 * <ol>
 *   <li>carga acumulada ≤ capacidad de peso y volumen del vehículo;</li>
 *   <li>pedidos con frío sólo en vehículos refrigerados;</li>
 *   <li>hora de llegada a cada parada dentro de su ventana horaria (si llega antes, espera);</li>
 *   <li>regreso al centro dentro del turno del repartidor;</li>
 *   <li>km totales (incluida la vuelta) ≤ autonomía del vehículo.</li>
 * </ol>
 * El repartidor sale del centro al inicio de su turno.
 * duracionMin = tiempo de viaje + tiempo de servicio por parada (no incluye esperas).
 */
public class CalculadoraRuta {

    /**
     * Simula la ruta y devuelve sus métricas. Si alguna restricción no se cumple,
     * {@link EvaluacionRuta#violacion()} indica la primera encontrada (las métricas se calculan igual,
     * lo que permite recalcular una ruta existente aunque haya dejado de ser estrictamente factible).
     */
    public EvaluacionRuta evaluar(List<PedidoPendienteDTO> secuencia,
                                  VehiculoPlan vehiculo,
                                  RepartidorPlan repartidor,
                                  ParametrosPlan params,
                                  MatrizDistancias matriz) {
        MotivoNoAsignable violacion = null;

        // 1 y 2: frío y capacidad (no dependen del orden)
        double peso = 0;
        double volumen = 0;
        for (PedidoPendienteDTO p : secuencia) {
            if (p.requiereFrio() && !vehiculo.tieneFrio() && violacion == null) {
                violacion = MotivoNoAsignable.SIN_VEHICULO_REFRIGERADO;
            }
            peso += p.pesoKg();
            volumen += p.volumenM3();
        }
        if (violacion == null && (peso > vehiculo.pesoMaxKg() + 1e-9 || volumen > vehiculo.volMaxM3() + 1e-9)) {
            violacion = MotivoNoAsignable.CAPACIDAD_AGOTADA;
        }

        // 3: simulación en el tiempo
        LocalDateTime reloj = params.fecha().atTime(repartidor.turnoInicio());
        Coordenada anterior = params.centro();
        double km = 0;
        long segundosViaje = 0;
        boolean estimada = false;
        List<LocalDateTime> etas = new ArrayList<>(secuencia.size());

        for (PedidoPendienteDTO p : secuencia) {
            Coordenada destino = Coordenada.of(p.latitud(), p.longitud());
            int seg = matriz.segundos(anterior, destino);
            km += matriz.km(anterior, destino);
            segundosViaje += seg;
            estimada |= matriz.esEstimada(anterior, destino);

            reloj = reloj.plusSeconds(seg);
            if (reloj.isBefore(p.ventanaDesde())) {
                reloj = p.ventanaDesde();           // llega antes: espera a que abra la ventana
            }
            if (reloj.isAfter(p.ventanaHasta()) && violacion == null) {
                violacion = MotivoNoAsignable.VENTANA_HORARIA_INALCANZABLE;
            }
            etas.add(reloj);
            reloj = reloj.plusMinutes(params.tiempoServicioMin());
            anterior = destino;
        }

        // vuelta al centro
        int segVuelta = matriz.segundos(anterior, params.centro());
        km += matriz.km(anterior, params.centro());
        segundosViaje += segVuelta;
        estimada |= matriz.esEstimada(anterior, params.centro());
        reloj = reloj.plusSeconds(segVuelta);

        // 4: turno
        LocalDateTime finTurno = params.fecha().atTime(repartidor.turnoFin());
        if (reloj.isAfter(finTurno) && violacion == null) {
            violacion = MotivoNoAsignable.EXCEDE_TURNO;
        }

        // 5: autonomía
        if (km > vehiculo.autonomiaKm() + 1e-9 && violacion == null) {
            violacion = MotivoNoAsignable.EXCEDE_AUTONOMIA;
        }

        int duracionMin = (int) Math.round(segundosViaje / 60.0) + secuencia.size() * params.tiempoServicioMin();
        return new EvaluacionRuta(violacion, km, duracionMin, List.copyOf(etas), reloj, estimada);
    }
}
