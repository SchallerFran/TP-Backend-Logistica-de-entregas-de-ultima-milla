package utnfc.isi.back.logistica.despacho.service.planificacion;

import utnfc.isi.back.logistica.despacho.dto.internal.PedidoPendienteDTO;
import utnfc.isi.back.logistica.commons.Prioridad;
import utnfc.isi.back.logistica.despacho.geo.Coordenada;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del núcleo de planificación. No levantan Spring ni base de datos: el planificador es Java puro.
 * La matriz se arma con distancias "a vuelo de pájaro" para que los números sean predecibles.
 */
class PlanificadorRutasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final Coordenada CENTRO = Coordenada.of(-31.4135, -64.1810);   // Córdoba centro
    private static final ParametrosPlan PARAMS = new ParametrosPlan(HOY, CENTRO, 30, 5);

    private final PlanificadorRutas planificador = new PlanificadorRutas(new CalculadoraRuta());

    // ------------------------------------------------------------------ helpers

    private static PedidoPendienteDTO pedido(long id, double lat, double lng, double kg, Prioridad prioridad,
                                             LocalTime desde, LocalTime hasta, boolean frio) {
        return new PedidoPendienteDTO(id, 1L, lat, lng, kg, 0.1, prioridad,
                HOY.atTime(desde), HOY.atTime(hasta), frio, 0);
    }

    private static PedidoPendienteDTO normal(long id, double lat, double lng, double kg) {
        return pedido(id, lat, lng, kg, Prioridad.NORMAL, LocalTime.of(8, 0), LocalTime.of(18, 0), false);
    }

    private static VehiculoPlan vehiculo(long id, double kg, double costoKm, boolean frio) {
        return new VehiculoPlan(id, "AA" + id, kg, 10, 300, costoKm, frio);
    }

    private static RepartidorPlan repartidor(long id) {
        return new RepartidorPlan(id, "R" + id, LocalTime.of(8, 0), LocalTime.of(17, 0));
    }

    /** Matriz simétrica a partir de Haversine: 1 km = 2 min (30 km/h). */
    private static MatrizDistancias matriz(List<PedidoPendienteDTO> pedidos) {
        LinkedHashSet<Coordenada> set = new LinkedHashSet<>();
        set.add(CENTRO.redondeada());
        pedidos.forEach(p -> set.add(Coordenada.of(p.latitud(), p.longitud()).redondeada()));
        List<Coordenada> puntos = new ArrayList<>(set);
        int n = puntos.size();
        int[][] m = new int[n][n];
        int[][] s = new int[n][n];
        FuenteDistancia[][] f = new FuenteDistancia[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double km = puntos.get(i).distanciaHaversineKm(puntos.get(j));
                m[i][j] = (int) Math.round(km * 1000);
                s[i][j] = (int) Math.round(km * 120);
                f[i][j] = i == j ? FuenteDistancia.MISMO_PUNTO : FuenteDistancia.GOOGLE;
            }
        }
        return new MatrizDistancias(puntos, m, s, f);
    }

    private ResultadoPlanificacion planificar(List<PedidoPendienteDTO> pedidos, List<VehiculoPlan> vehiculos,
                                              List<RepartidorPlan> repartidores) {
        return planificador.planificar(pedidos, vehiculos, repartidores, PARAMS, matriz(pedidos));
    }

    // ------------------------------------------------------------------ tests

    @Test
    void agrupaPedidosCercanosEnUnaSolaRutaConElVehiculoMasBarato() {
        var pedidos = List.of(normal(1, -31.42, -64.19, 5), normal(2, -31.43, -64.20, 5));
        var res = planificar(pedidos, List.of(vehiculo(1, 100, 900, false), vehiculo(2, 100, 500, false)),
                List.of(repartidor(1), repartidor(2)));

        assertEquals(1, res.rutas().size());
        assertTrue(res.noAsignados().isEmpty());
        RutaPlan ruta = res.rutas().get(0);
        assertEquals(2L, ruta.vehiculo().id(), "debe elegir el vehículo con menor costo por km");
        assertEquals(2, ruta.paradas().size());
        assertEquals(ruta.kmTotal() * 500, ruta.costoTotal(), 1e-6);
    }

    @Test
    void urgenteTienePrioridadCuandoNoHayCapacidadParaAmbos() {
        var normal = normal(1, -31.42, -64.19, 8);
        var urgente = pedido(2, -31.43, -64.20, 8, Prioridad.URGENTE, LocalTime.of(8, 0), LocalTime.of(18, 0), false);
        var res = planificar(List.of(normal, urgente), List.of(vehiculo(1, 10, 500, false)), List.of(repartidor(1)));

        assertEquals(1, res.rutas().size());
        assertEquals(2L, res.rutas().get(0).paradas().get(0).pedido().id());
        assertEquals(1, res.noAsignados().size());
        assertEquals(1L, res.noAsignados().get(0).pedido().id());
        assertEquals(MotivoNoAsignable.CAPACIDAD_AGOTADA, res.noAsignados().get(0).motivo());
    }

    @Test
    void pedidoConFrioSinVehiculoRefrigeradoNoEsAsignable() {
        var frio = pedido(1, -31.42, -64.19, 5, Prioridad.NORMAL, LocalTime.of(8, 0), LocalTime.of(18, 0), true);
        var res = planificar(List.of(frio), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        assertTrue(res.rutas().isEmpty());
        assertEquals(MotivoNoAsignable.SIN_VEHICULO_REFRIGERADO, res.noAsignados().get(0).motivo());
    }

    @Test
    void noGastaElVehiculoRefrigeradoEnPedidosSinFrioAIgualCosto() {
        var comun = normal(1, -31.42, -64.19, 5);
        var res = planificar(List.of(comun),
                List.of(vehiculo(1, 100, 500, true), vehiculo(2, 100, 500, false)),
                List.of(repartidor(1), repartidor(2)));

        assertEquals(2L, res.rutas().get(0).vehiculo().id());
    }

    @Test
    void destinoFueraDeCoberturaNoEsAsignable() {
        var lejos = normal(1, -32.9471, -60.6985, 5);   // Rosario
        var res = planificar(List.of(lejos), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        assertEquals(MotivoNoAsignable.FUERA_DE_COBERTURA, res.noAsignados().get(0).motivo());
    }

    @Test
    void ventanaInalcanzableNoEsAsignable() {
        // ~5 km de viaje = ~10 min; la ventana cierra a las 08:03 y el turno empieza 08:00
        var apurado = pedido(1, -31.4135, -64.2340, 5, Prioridad.NORMAL,
                LocalTime.of(8, 0), LocalTime.of(8, 3), false);
        var res = planificar(List.of(apurado), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        assertEquals(MotivoNoAsignable.VENTANA_HORARIA_INALCANZABLE, res.noAsignados().get(0).motivo());
    }

    @Test
    void siLlegaAntesDeLaVentanaEsperaYLaEtaEsLaAperturaDeLaVentana() {
        var tarde = pedido(1, -31.42, -64.19, 5, Prioridad.NORMAL, LocalTime.of(10, 0), LocalTime.of(12, 0), false);
        var res = planificar(List.of(tarde), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        assertEquals(LocalDateTime.of(HOY, LocalTime.of(10, 0)), res.rutas().get(0).paradas().get(0).eta());
    }

    @Test
    void respetaElTurnoDelRepartidor() {
        var tarde = pedido(1, -31.42, -64.19, 5, Prioridad.NORMAL, LocalTime.of(16, 58), LocalTime.of(18, 0), false);
        var res = planificar(List.of(tarde), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        assertEquals(MotivoNoAsignable.EXCEDE_TURNO, res.noAsignados().get(0).motivo());
    }

    @Test
    void sinRecursosLibresTodosQuedanNoAsignables() {
        var res = planificar(List.of(normal(1, -31.42, -64.19, 5)), List.of(), List.of());

        assertEquals(MotivoNoAsignable.SIN_RECURSOS_DISPONIBLES, res.noAsignados().get(0).motivo());
    }

    @Test
    void abreOtraRutaCuandoLaPrimeraSeLlena() {
        var pedidos = List.of(normal(1, -31.42, -64.19, 8), normal(2, -31.43, -64.20, 8));
        var res = planificar(pedidos,
                List.of(vehiculo(1, 10, 500, false), vehiculo(2, 10, 600, false)),
                List.of(repartidor(1), repartidor(2)));

        assertEquals(2, res.rutas().size());
        assertTrue(res.noAsignados().isEmpty());
        assertNotEquals(res.rutas().get(0).repartidor().id(), res.rutas().get(1).repartidor().id());
    }

    @Test
    void ordenaLasParadasRespetandoVentanas() {
        // el pedido 1 está más cerca pero su ventana abre tarde; el 2 tiene que ir primero
        var p1 = pedido(1, -31.42, -64.19, 1, Prioridad.NORMAL, LocalTime.of(14, 0), LocalTime.of(16, 0), false);
        var p2 = pedido(2, -31.45, -64.22, 1, Prioridad.NORMAL, LocalTime.of(8, 0), LocalTime.of(9, 0), false);
        var res = planificar(List.of(p1, p2), List.of(vehiculo(1, 100, 500, false)), List.of(repartidor(1)));

        var paradas = res.rutas().get(0).paradas();
        assertEquals(2L, paradas.get(0).pedido().id());
        assertEquals(1L, paradas.get(1).pedido().id());
        assertTrue(paradas.get(0).eta().isBefore(paradas.get(1).eta()));
    }
}
