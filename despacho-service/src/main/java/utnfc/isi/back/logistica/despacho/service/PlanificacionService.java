package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.dto.internal.*;
import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.exception.ServicioNoDisponibleException;
import utnfc.isi.back.logistica.commons.Coordenada;
import utnfc.isi.back.logistica.despacho.client.PedidosClient;
import utnfc.isi.back.logistica.despacho.client.PedidosNoDisponibleException;
import utnfc.isi.back.logistica.despacho.client.PedidosRechazoException;
import utnfc.isi.back.logistica.despacho.config.DespachoProperties;
import utnfc.isi.back.logistica.despacho.dto.*;
import utnfc.isi.back.logistica.despacho.model.*;
import utnfc.isi.back.logistica.despacho.repository.CentroDistribucionRepository;
import utnfc.isi.back.logistica.despacho.repository.RepartidorRepository;
import utnfc.isi.back.logistica.despacho.repository.RutaRepository;
import utnfc.isi.back.logistica.despacho.repository.VehiculoRepository;
import utnfc.isi.back.logistica.despacho.service.planificacion.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

/**
 * Proceso de asignación (corazón del dominio) y su colaboración con pedidos-service.
 * <p>
 * Se resuelve como una pequeña <b>saga</b> en dos pasos, porque cada servicio tiene su propia base y no
 * hay transacción distribuida:
 * <ol>
 *   <li>despacho arma las rutas y las persiste en <b>BORRADOR</b> (no se las muestra al repartidor);</li>
 *   <li>le pide a pedidos-service, en una sola llamada, que pase los pedidos a ASIGNADO / NO_ASIGNABLE.
 *       Con la respuesta, las rutas pasan a <b>CONFIRMADA</b>, quitando los pedidos que pedidos rechazó
 *       (por ejemplo, cancelados mientras se planificaba).</li>
 * </ol>
 * Si pedidos-service no responde en el paso 2, las rutas quedan en BORRADOR (no bloquean el día: se
 * borran al volver a planificar) y el despachante puede reintentar sólo la confirmación.
 * <p>
 * Las llamadas HTTP se hacen FUERA de las transacciones de base (TransactionTemplate en tramos cortos):
 * no se mantiene una conexión a la base abierta mientras se espera a otro servicio o a Google.
 */
@Slf4j
@Service
public class PlanificacionService {

    private final CentroDistribucionRepository centroRepository;
    private final VehiculoRepository vehiculoRepository;
    private final RepartidorRepository repartidorRepository;
    private final RutaRepository rutaRepository;
    private final PedidosClient pedidosClient;
    private final DistanciaService distanciaService;
    private final PlanificadorRutas planificador;
    private final RutaRecalculador recalculador;
    private final DespachoProperties props;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final TransactionTemplate txLectura;

    public PlanificacionService(CentroDistribucionRepository centroRepository,
                                VehiculoRepository vehiculoRepository,
                                RepartidorRepository repartidorRepository,
                                RutaRepository rutaRepository,
                                PedidosClient pedidosClient,
                                DistanciaService distanciaService,
                                PlanificadorRutas planificador,
                                RutaRecalculador recalculador,
                                DespachoProperties props,
                                Clock clock,
                                PlatformTransactionManager txManager) {
        this.centroRepository = centroRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.repartidorRepository = repartidorRepository;
        this.rutaRepository = rutaRepository;
        this.pedidosClient = pedidosClient;
        this.distanciaService = distanciaService;
        this.planificador = planificador;
        this.recalculador = recalculador;
        this.props = props;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
        this.txLectura = new TransactionTemplate(txManager);
        this.txLectura.setReadOnly(true);
    }

    // =============================================================================================
    // Planificar
    // =============================================================================================

    public PlanificacionResponse planificar(PlanificacionRequest req) {
        Long centroId = req.centroId();
        LocalDate fecha = req.fecha();
        if (fecha.isBefore(LocalDate.now(clock))) {
            throw new ReglaNegocioException("FECHA_PASADA", "No se puede planificar una fecha pasada: " + fecha);
        }

        // 0) las rutas BORRADOR de un intento anterior se descartan: no bloquean el día
        tx.executeWithoutResult(s -> borrarBorradores(centroId, fecha));

        // 1) centro y recursos libres para la fecha (lectura corta)
        Contexto ctx = txLectura.execute(s -> cargarContexto(centroId, fecha));

        // 2) pedidos pendientes: sin pedidos-service no se puede planificar (no hay réplica local)
        List<PedidoPendienteDTO> pendientes;
        try {
            pendientes = pedidosClient.obtenerPendientes(centroId, fecha).stream()
                    .filter(p -> centroId.equals(p.centroId()))
                    .toList();
        } catch (PedidosNoDisponibleException e) {
            throw new ServicioNoDisponibleException("pedidos-service",
                    "No se pudieron obtener los pedidos pendientes. Reintente más tarde.", e);
        } catch (PedidosRechazoException e) {
            throw new ReglaNegocioException("PEDIDOS_RECHAZO", e.getMessage());
        }
        if (pendientes.isEmpty()) {
            return new PlanificacionResponse(centroId, fecha, EstadoPlanificacion.SIN_RUTAS, 0, List.of(), List.of(),
                    List.of(), Map.of(), List.of("No hay pedidos pendientes para el centro y la fecha"));
        }

        // 3) distancias reales (Google / caché / estimación)
        List<Coordenada> puntos = new ArrayList<>();
        puntos.add(ctx.params().centro());
        pendientes.forEach(p -> puntos.add(Coordenada.of(p.latitud(), p.longitud())));
        MatrizDistancias matriz = distanciaService.matriz(puntos);

        // 4) planificación (Java puro)
        ResultadoPlanificacion resultado = planificador.planificar(pendientes, ctx.vehiculos(), ctx.repartidores(),
                ctx.params(), matriz);
        log.info("Planificación centro={} fecha={}: {} pedidos → {} rutas, {} no asignables",
                centroId, fecha, pendientes.size(), resultado.rutas().size(), resultado.noAsignados().size());

        // 5) persistir en BORRADOR
        List<Long> rutaIds;
        try {
            rutaIds = tx.execute(s -> persistirBorradores(resultado, fecha));
        } catch (DataIntegrityViolationException e) {
            throw new ReglaNegocioException("PLANIFICACION_CONCURRENTE",
                    "Otro despachante está planificando el mismo centro y fecha. Reintente.");
        }

        // 6) confirmar con pedidos-service
        List<PedidoNoAsignableDTO> noAsignables = resultado.noAsignados().stream()
                .map(n -> new PedidoNoAsignableDTO(n.pedido().id(), n.motivo().name(), n.detalle()))
                .toList();
        List<String> advertencias = new ArrayList<>();
        Map<FuenteDistancia, Integer> fuentes = matriz.resumenFuentes();
        if (fuentes.getOrDefault(FuenteDistancia.ESTIMADA, 0) > 0) {
            advertencias.add("Algunas distancias son estimadas (Google no disponible): revisar rutas marcadas como estimadas");
        }
        if (fuentes.getOrDefault(FuenteDistancia.CACHE_VENCIDA, 0) > 0) {
            advertencias.add("Se usaron distancias de caché vencida porque Google no respondió");
        }

        Confirmacion conf = confirmar(centroId, fecha, rutaIds, noAsignables, matriz);
        advertencias.addAll(conf.advertencias());

        return new PlanificacionResponse(centroId, fecha, conf.estado(), pendientes.size(), conf.rutas(),
                resultado.noAsignados().stream()
                        .map(n -> new NoAsignableResponse(n.pedido().id(), n.motivo().name(), n.detalle()))
                        .toList(),
                conf.rechazados(), fuentes, advertencias);
    }

    // =============================================================================================
    // Reintentar confirmación (cuando pedidos-service no respondió)
    // =============================================================================================

    public PlanificacionResponse reintentarConfirmacion(PlanificacionRequest req) {
        List<Long> ids = txLectura.execute(s ->
                rutaRepository.findByCentroFechaEstado(req.centroId(), req.fecha(), EstadoRuta.BORRADOR).stream()
                        .map(Ruta::getId).toList());
        if (ids == null || ids.isEmpty()) {
            throw new ReglaNegocioException("SIN_BORRADORES",
                    "No hay rutas en BORRADOR para el centro y la fecha indicados");
        }
        // En el reintento no se reenvían los no asignables: siguen RECIBIDO y entran en la próxima planificación.
        Confirmacion conf = confirmar(req.centroId(), req.fecha(), ids, List.of(), null);
        int paradas = conf.rutas().stream().mapToInt(r -> r.paradas().size()).sum() + conf.rechazados().size();
        return new PlanificacionResponse(req.centroId(), req.fecha(), conf.estado(), paradas, conf.rutas(), List.of(),
                conf.rechazados(), Map.of(), conf.advertencias());
    }

    /** Descarta una ruta en BORRADOR (por ejemplo, si el despachante prefiere replanificar más tarde). */
    public void descartarBorrador(Long rutaId) {
        tx.executeWithoutResult(s -> {
            Ruta ruta = rutaRepository.findById(rutaId)
                    .orElseThrow(() -> RecursoNoEncontradoException.de("Ruta", rutaId));
            if (ruta.getEstado() != EstadoRuta.BORRADOR) {
                throw new ReglaNegocioException("RUTA_NO_BORRADOR",
                        "Sólo se pueden descartar rutas en BORRADOR (estado actual: " + ruta.getEstado() + ")");
            }
            rutaRepository.delete(ruta);
        });
    }

    /** Limpieza diaria: borradores de días pasados que nunca se confirmaron. */
    @Scheduled(cron = "0 15 3 * * *", zone = "${despacho.zona-horaria}")
    public void limpiarBorradoresVencidos() {
        tx.executeWithoutResult(s -> {
            LocalDate hoy = LocalDate.now(clock);
            List<Ruta> viejas = rutaRepository.findAll().stream()
                    .filter(r -> r.getEstado() == EstadoRuta.BORRADOR && r.getFecha().isBefore(hoy))
                    .toList();
            if (!viejas.isEmpty()) {
                log.info("Eliminando {} rutas en BORRADOR de días pasados", viejas.size());
                rutaRepository.deleteAll(viejas);
            }
        });
    }

    // =============================================================================================
    // Paso 2 de la saga
    // =============================================================================================

    private record Confirmacion(EstadoPlanificacion estado, List<RutaResponse> rutas,
                                List<PedidoRechazadoDTO> rechazados, List<String> advertencias) {
    }

    private Confirmacion confirmar(Long centroId, LocalDate fecha, List<Long> rutaIds,
                                   List<PedidoNoAsignableDTO> noAsignables, MatrizDistancias matriz) {
        if (rutaIds.isEmpty() && noAsignables.isEmpty()) {
            return new Confirmacion(EstadoPlanificacion.SIN_RUTAS, List.of(), List.of(), List.of());
        }

        List<RutaAsignadaDTO> rutas = txLectura.execute(s -> rutaIds.stream()
                .map(id -> rutaRepository.findById(id).orElseThrow())
                .map(r -> new RutaAsignadaDTO(r.getId(), r.getParadas().stream().map(Parada::getPedidoId).toList()))
                .toList());

        ConfirmacionAsignacionResponse respuesta;
        try {
            respuesta = pedidosClient.confirmarAsignacion(
                    new ConfirmacionAsignacionRequest(centroId, fecha, rutas, noAsignables));
        } catch (PedidosNoDisponibleException e) {
            log.warn("pedidos-service no respondió la confirmación; {} rutas quedan en BORRADOR", rutaIds.size());
            return new Confirmacion(EstadoPlanificacion.PENDIENTE_CONFIRMACION, mapear(rutaIds), List.of(),
                    List.of("pedidos-service no respondió: las rutas quedaron en BORRADOR. "
                            + "Reintentar con POST /api/despacho/planificaciones/confirmacion"));
        } catch (PedidosRechazoException e) {
            tx.executeWithoutResult(s -> rutaIds.forEach(id -> rutaRepository.findById(id).ifPresent(rutaRepository::delete)));
            throw new ReglaNegocioException("CONFIRMACION_RECHAZADA",
                    "pedidos-service rechazó la confirmación; se descartaron las rutas. " + e.getMessage());
        }

        Set<Long> confirmados = new HashSet<>(Optional.ofNullable(respuesta.confirmados()).orElse(List.of()));
        List<PedidoRechazadoDTO> rechazados = Optional.ofNullable(respuesta.rechazados()).orElse(List.of());

        List<Long> vigentes = tx.execute(s -> {
            List<Long> quedan = new ArrayList<>();
            for (Long id : rutaIds) {
                Ruta ruta = rutaRepository.findConRecursos(id).orElseThrow();
                // Sólo quedan las paradas cuyo pedido fue confirmado como ASIGNADO
                List<Long> sinConfirmar = ruta.getParadas().stream()
                        .map(Parada::getPedidoId)
                        .filter(pid -> !confirmados.contains(pid))
                        .toList();
                boolean sigue = sinConfirmar.isEmpty() || recalculador.quitarParadas(ruta, sinConfirmar, matriz);
                if (sigue) {
                    ruta.setEstado(EstadoRuta.CONFIRMADA);
                    quedan.add(id);
                }
            }
            return quedan;
        });

        List<String> advertencias = new ArrayList<>();
        if (!rechazados.isEmpty()) {
            advertencias.add(rechazados.size() + " pedido(s) cambiaron de estado durante la planificación y se "
                    + "quitaron de sus rutas");
        }
        EstadoPlanificacion estado = vigentes.isEmpty() ? EstadoPlanificacion.SIN_RUTAS : EstadoPlanificacion.CONFIRMADA;
        return new Confirmacion(estado, mapear(vigentes), rechazados, advertencias);
    }

    // =============================================================================================
    // Auxiliares
    // =============================================================================================

    private record Contexto(ParametrosPlan params, List<VehiculoPlan> vehiculos, List<RepartidorPlan> repartidores) {
    }

    private Contexto cargarContexto(Long centroId, LocalDate fecha) {
        CentroDistribucion centro = centroRepository.findById(centroId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Centro", centroId));
        if (!centro.isActivo()) {
            throw new ReglaNegocioException("CENTRO_INACTIVO", "El centro " + centro.getNombre() + " está inactivo");
        }
        ParametrosPlan params = new ParametrosPlan(fecha, centro.coordenada(), centro.getRadioCoberturaKm(),
                props.planificacion().tiempoServicioMin());

        // Vehículos y repartidores sin ruta ese día (los BORRADOR ya se borraron en el paso 0)
        List<VehiculoPlan> vehiculos = new ArrayList<>(vehiculoRepository.findLibresParaFecha(centroId, fecha).stream()
                .map(v -> new VehiculoPlan(v.getId(), v.getPatente(), v.getPesoMaxKg(), v.getVolMaxM3(),
                        v.getAutonomiaKm(), v.getCostoPorKm(), v.isTieneFrio()))
                .toList());
        List<RepartidorPlan> repartidores = new ArrayList<>(repartidorRepository.findLibresParaFecha(centroId, fecha)
                .stream()
                .map(r -> new RepartidorPlan(r.getId(), r.getNombre(), r.getTurnoInicio(), r.getTurnoFin()))
                .toList());

        vehiculos.sort(Comparator.comparingDouble(VehiculoPlan::costoPorKm).thenComparing(VehiculoPlan::id));
        repartidores.sort(Comparator.comparing(RepartidorPlan::turnoInicio).thenComparing(RepartidorPlan::id));
        return new Contexto(params, vehiculos, repartidores);
    }

    private void borrarBorradores(Long centroId, LocalDate fecha) {
        List<Ruta> borradores = rutaRepository.findByCentroFechaEstado(centroId, fecha, EstadoRuta.BORRADOR);
        if (!borradores.isEmpty()) {
            log.info("Descartando {} rutas en BORRADOR previas (centro={}, fecha={})", borradores.size(), centroId, fecha);
            rutaRepository.deleteAll(borradores);
            rutaRepository.flush();
        }
    }

    private List<Long> persistirBorradores(ResultadoPlanificacion resultado, LocalDate fecha) {
        List<Long> ids = new ArrayList<>();
        for (RutaPlan plan : resultado.rutas()) {
            Ruta ruta = new Ruta();
            ruta.setFecha(fecha);
            ruta.setEstado(EstadoRuta.BORRADOR);
            ruta.setVehiculo(vehiculoRepository.getReferenceById(plan.vehiculo().id()));
            ruta.setRepartidor(repartidorRepository.getReferenceById(plan.repartidor().id()));
            ruta.setKmTotal(plan.kmTotal());
            ruta.setDuracionMin(plan.duracionMin());
            ruta.setCostoTotal(plan.costoTotal());
            ruta.setEstimada(plan.estimada());
            for (ParadaPlan pp : plan.paradas()) {
                PedidoPendienteDTO p = pp.pedido();
                Parada parada = new Parada();
                parada.setPedidoId(p.id());
                parada.setLatitud(p.latitud());
                parada.setLongitud(p.longitud());
                parada.setPesoKg(p.pesoKg());
                parada.setVolumenM3(p.volumenM3());
                parada.setVentanaDesde(p.ventanaDesde());
                parada.setVentanaHasta(p.ventanaHasta());
                parada.setEtaEstimada(pp.eta());
                ruta.agregarParada(parada);
            }
            ids.add(rutaRepository.saveAndFlush(ruta).getId());
        }
        return ids;
    }

    private List<RutaResponse> mapear(List<Long> ids) {
        return txLectura.execute(s -> ids.stream()
                .map(id -> rutaRepository.findConRecursos(id).orElseThrow())
                .map(DespachoMapper::aResponse)
                .toList());
    }
}
