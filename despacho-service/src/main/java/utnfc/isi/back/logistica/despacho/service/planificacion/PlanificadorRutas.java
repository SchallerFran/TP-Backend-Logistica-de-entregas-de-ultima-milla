package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;
import utnfc.isi.back.logistica.commons.Prioridad;
import utnfc.isi.back.logistica.despacho.geo.Coordenada;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Heurística de inserción más barata ("cheapest insertion") con prioridades.
 * <p>
 * No busca el óptimo matemático (el problema es un VRP con ventanas horarias, NP-difícil) sino un
 * criterio razonable, explicable y que SIEMPRE respeta las restricciones:
 * <ol>
 *   <li>Ordena los pedidos: URGENTE primero; dentro de cada prioridad, el de ventana más temprana
 *       en cerrar (es el que menos margen tiene).</li>
 *   <li>Descarta de entrada los que no pueden ir en ningún caso (fuera de fecha, fuera de cobertura,
 *       requieren frío sin vehículo refrigerado, exceden la capacidad de cualquier vehículo).</li>
 *   <li>Para cada pedido evalúa: insertarlo en cada posición de cada ruta ya abierta, o abrir una
 *       ruta nueva con cada par (vehículo libre, repartidor libre). Descarta las opciones infactibles
 *       (CalculadoraRuta) y elige la de menor costo = km adicionales × costo por km del vehículo.</li>
 *   <li>Si no hay ninguna opción factible, el pedido queda NO asignable con el motivo predominante.</li>
 * </ol>
 * Cada vehículo y cada repartidor se usan en a lo sumo una ruta (regla del dominio).
 */
public class PlanificadorRutas {

    private final CalculadoraRuta calculadora;

    public PlanificadorRutas(CalculadoraRuta calculadora) {
        this.calculadora = calculadora;
    }

    public static final Comparator<PedidoPendienteDTO> ORDEN_DE_ATENCION = Comparator
            .comparing((PedidoPendienteDTO p) -> p.prioridad() == Prioridad.URGENTE ? 0 : 1)
            .thenComparing(PedidoPendienteDTO::ventanaHasta)
            .thenComparing(PedidoPendienteDTO::ventanaDesde)
            .thenComparing(PedidoPendienteDTO::id);

    public ResultadoPlanificacion planificar(List<PedidoPendienteDTO> pedidos,
                                             List<VehiculoPlan> vehiculos,
                                             List<RepartidorPlan> repartidores,
                                             ParametrosPlan params,
                                             MatrizDistancias matriz) {

        List<PedidoPendienteDTO> ordenados = pedidos.stream().sorted(ORDEN_DE_ATENCION).toList();
        List<VehiculoPlan> vehiculosLibres = new ArrayList<>(vehiculos);
        List<RepartidorPlan> repartidoresLibres = new ArrayList<>(repartidores);
        List<RutaEnConstruccion> rutas = new ArrayList<>();
        List<NoAsignado> noAsignados = new ArrayList<>();

        for (PedidoPendienteDTO pedido : ordenados) {
            Optional<MotivoNoAsignable> descarte = prefiltrar(pedido, vehiculos, params);
            if (descarte.isPresent()) {
                noAsignados.add(new NoAsignado(pedido, descarte.get(), descarte.get().descripcion()));
                continue;
            }

            Opcion mejor = null;
            Map<MotivoNoAsignable, Integer> rechazos = new EnumMap<>(MotivoNoAsignable.class);

            // a) insertar en una ruta ya abierta
            for (RutaEnConstruccion ruta : rutas) {
                for (int pos = 0; pos <= ruta.secuencia.size(); pos++) {
                    List<PedidoPendienteDTO> candidata = new ArrayList<>(ruta.secuencia);
                    candidata.add(pos, pedido);
                    EvaluacionRuta ev = calculadora.evaluar(candidata, ruta.vehiculo, ruta.repartidor, params, matriz);
                    if (!ev.factible()) {
                        rechazos.merge(ev.violacion(), 1, Integer::sum);
                        continue;
                    }
                    double costo = (ev.kmTotal() - ruta.evaluacion.kmTotal()) * ruta.vehiculo.costoPorKm();
                    mejor = elegir(mejor, new Opcion(ruta, null, null, candidata, ev, costo, false));
                }
            }

            // b) abrir una ruta nueva con un par vehículo/repartidor libre
            for (VehiculoPlan v : vehiculosLibres) {
                for (RepartidorPlan r : repartidoresLibres) {
                    List<PedidoPendienteDTO> candidata = List.of(pedido);
                    EvaluacionRuta ev = calculadora.evaluar(candidata, v, r, params, matriz);
                    if (!ev.factible()) {
                        rechazos.merge(ev.violacion(), 1, Integer::sum);
                        continue;
                    }
                    double costo = ev.kmTotal() * v.costoPorKm();
                    // no "gastar" un vehículo refrigerado en un pedido que no lo necesita, a igual costo
                    boolean desperdiciaFrio = v.tieneFrio() && !pedido.requiereFrio();
                    mejor = elegir(mejor, new Opcion(null, v, r, new ArrayList<>(candidata), ev, costo, desperdiciaFrio));
                }
            }

            if (mejor == null) {
                noAsignados.add(noAsignable(pedido, rechazos));
                continue;
            }

            // aplicar la mejor opción
            if (mejor.ruta != null) {
                mejor.ruta.secuencia = mejor.secuencia;
                mejor.ruta.evaluacion = mejor.evaluacion;
            } else {
                RutaEnConstruccion nueva = new RutaEnConstruccion(mejor.vehiculo, mejor.repartidor);
                nueva.secuencia = mejor.secuencia;
                nueva.evaluacion = mejor.evaluacion;
                rutas.add(nueva);
                vehiculosLibres.remove(mejor.vehiculo);
                repartidoresLibres.remove(mejor.repartidor);
            }
        }

        List<RutaPlan> resultado = rutas.stream().map(this::aRutaPlan).toList();
        return new ResultadoPlanificacion(resultado, noAsignados);
    }

    /** Reglas que descartan un pedido sin importar cómo se armen las rutas. */
    private Optional<MotivoNoAsignable> prefiltrar(PedidoPendienteDTO p, List<VehiculoPlan> vehiculos,
                                                   ParametrosPlan params) {
        LocalDate fecha = params.fecha();
        if (p.ventanaHasta().toLocalDate().isBefore(fecha) || p.ventanaDesde().toLocalDate().isAfter(fecha)) {
            return Optional.of(MotivoNoAsignable.VENTANA_FUERA_DE_FECHA);
        }
        Coordenada destino = Coordenada.of(p.latitud(), p.longitud());
        if (!destino.estaDentroDelRadio(params.centro(), params.radioCoberturaKm())) {
            return Optional.of(MotivoNoAsignable.FUERA_DE_COBERTURA);
        }
        if (vehiculos.isEmpty()) {
            return Optional.of(MotivoNoAsignable.SIN_RECURSOS_DISPONIBLES);
        }
        List<VehiculoPlan> compatibles = vehiculos.stream()
                .filter(v -> !p.requiereFrio() || v.tieneFrio())
                .toList();
        if (compatibles.isEmpty()) {
            return Optional.of(MotivoNoAsignable.SIN_VEHICULO_REFRIGERADO);
        }
        boolean entraEnAlguno = compatibles.stream()
                .anyMatch(v -> p.pesoKg() <= v.pesoMaxKg() && p.volumenM3() <= v.volMaxM3());
        if (!entraEnAlguno) {
            return Optional.of(MotivoNoAsignable.EXCEDE_CAPACIDAD_VEHICULOS);
        }
        return Optional.empty();
    }

    private static Opcion elegir(Opcion actual, Opcion candidata) {
        if (actual == null) {
            return candidata;
        }
        int cmp = Double.compare(candidata.costo, actual.costo);
        if (cmp != 0) {
            return cmp < 0 ? candidata : actual;
        }
        if (candidata.desperdiciaFrio != actual.desperdiciaFrio) {
            return candidata.desperdiciaFrio ? actual : candidata;
        }
        // a igual costo, preferir sumar a una ruta existente antes que abrir otra
        if ((candidata.ruta != null) != (actual.ruta != null)) {
            return candidata.ruta != null ? candidata : actual;
        }
        return actual; // determinístico: gana la primera evaluada
    }

    /** Elige el motivo más frecuente entre las opciones que se evaluaron y fallaron. */
    private static NoAsignado noAsignable(PedidoPendienteDTO pedido, Map<MotivoNoAsignable, Integer> rechazos) {
        if (rechazos.isEmpty()) {
            return new NoAsignado(pedido, MotivoNoAsignable.SIN_RECURSOS_DISPONIBLES,
                    MotivoNoAsignable.SIN_RECURSOS_DISPONIBLES.descripcion());
        }
        MotivoNoAsignable predominante = rechazos.entrySet().stream()
                .max(Map.Entry.<MotivoNoAsignable, Integer>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .orElseThrow()
                .getKey();
        int total = rechazos.values().stream().mapToInt(Integer::intValue).sum();
        String detalle = predominante.descripcion() + ". Opciones evaluadas: " + total + " ("
                + rechazos.entrySet().stream()
                .map(e -> e.getKey().name() + "=" + e.getValue())
                .collect(Collectors.joining(", ")) + ")";
        return new NoAsignado(pedido, predominante, detalle);
    }

    private RutaPlan aRutaPlan(RutaEnConstruccion r) {
        List<ParadaPlan> paradas = new ArrayList<>();
        for (int i = 0; i < r.secuencia.size(); i++) {
            paradas.add(new ParadaPlan(r.secuencia.get(i), i + 1, r.evaluacion.etas().get(i)));
        }
        EvaluacionRuta ev = r.evaluacion;
        return new RutaPlan(r.vehiculo, r.repartidor, List.copyOf(paradas), ev.kmTotal(), ev.duracionMin(),
                ev.kmTotal() * r.vehiculo.costoPorKm(), ev.estimada());
    }

    private static final class RutaEnConstruccion {
        private final VehiculoPlan vehiculo;
        private final RepartidorPlan repartidor;
        private List<PedidoPendienteDTO> secuencia = new ArrayList<>();
        private EvaluacionRuta evaluacion;

        private RutaEnConstruccion(VehiculoPlan vehiculo, RepartidorPlan repartidor) {
            this.vehiculo = vehiculo;
            this.repartidor = repartidor;
        }
    }

    private record Opcion(RutaEnConstruccion ruta, VehiculoPlan vehiculo, RepartidorPlan repartidor,
                          List<PedidoPendienteDTO> secuencia, EvaluacionRuta evaluacion, double costo,
                          boolean desperdiciaFrio) {
    }
}
