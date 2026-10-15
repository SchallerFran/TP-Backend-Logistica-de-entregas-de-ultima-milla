package utnfc.isi.back.logistica.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utnfc.isi.back.logistica.pedidos.client.DespachoClient;
import utnfc.isi.back.logistica.pedidos.dto.CambioEstadoDTO;
import utnfc.isi.back.logistica.pedidos.dto.CentroDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearPedidoRequest;
import utnfc.isi.back.logistica.pedidos.dto.DestinatarioDTO;
import utnfc.isi.back.logistica.pedidos.dto.EstadoActualDTO;
import utnfc.isi.back.logistica.pedidos.dto.PedidoDTO;
import utnfc.isi.back.logistica.pedidos.dto.PedidoPendienteDTO;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.model.Destinatario;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.repository.CambioEstadoRepository;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta los casos de uso del pedido. Las reglas de negocio viven en la
 * entidad Pedido: aca solo se resuelven permisos, persistencia e integracion.
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final CambioEstadoRepository cambioEstadoRepository;
    private final DespachoClient despachoClient;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         CambioEstadoRepository cambioEstadoRepository,
                         DespachoClient despachoClient) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.cambioEstadoRepository = cambioEstadoRepository;
        this.despachoClient = despachoClient;
    }

    @Transactional
    public PedidoDTO crear(String keycloakUserId, CrearPedidoRequest request) {
        // 1. perfil
        Cliente cliente = clienteRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new IllegalArgumentException("Debe registrar su perfil de cliente"));

        // 2. cliente habilitado (regla del modelo)
        if (!cliente.puedeCrearPedidos()) {
            throw new IllegalArgumentException("El cliente esta desactivado y no puede crear pedidos");
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDestinatario(aDestinatario(request.destinatario()));
        pedido.setCentroId(request.centroId());
        pedido.setPesoKg(request.pesoKg());
        pedido.setVolumenM3(request.volumenM3());
        pedido.setPrioridad(request.prioridad());
        pedido.setVentanaDesde(request.ventanaDesde());
        pedido.setVentanaHasta(request.ventanaHasta());
        pedido.setRequiereFrio(request.requiereFrio());

        // 3. ventana (regla del modelo)
        if (!pedido.ventanaValida()) {
            throw new IllegalArgumentException("La ventana de entrega es invalida: ventanaHasta debe ser posterior a ventanaDesde");
        }

        // 4. centro: 422 si no existe o esta inactivo, 503 si despacho no responde
        CentroDTO centro = despachoClient.obtenerCentro(request.centroId());
        if (!centro.activo()) {
            throw new IllegalArgumentException("El centro de distribucion " + centro.id() + " esta inactivo");
        }

        return PedidoDTO.from(pedidoRepository.save(pedido));
    }

    @Transactional(readOnly = true)
    public Page<PedidoDTO> listar(String keycloakUserId, boolean soloPropios,
                                  EstadoPedido estado, Long centroId, Pageable pageable) {
        List<Specification<Pedido>> filtros = new ArrayList<>();
        if (estado != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("estado"), estado));
        }
        if (centroId != null) {
            filtros.add((root, query, cb) -> cb.equal(root.get("centroId"), centroId));
        }
        if (soloPropios) {
            filtros.add((root, query, cb) ->
                    cb.equal(root.get("cliente").get("keycloakUserId"), keycloakUserId));
        }
        return pedidoRepository.findAll(Specification.allOf(filtros), pageable).map(PedidoDTO::from);
    }

    @Transactional(readOnly = true)
    public PedidoDTO obtener(Long id, String keycloakUserId, boolean soloPropios) {
        return PedidoDTO.from(buscarConAcceso(id, keycloakUserId, soloPropios));
    }

    @Transactional(readOnly = true)
    public List<CambioEstadoDTO> historial(Long id, String keycloakUserId, boolean soloPropios) {
        buscarConAcceso(id, keycloakUserId, soloPropios);
        return cambioEstadoRepository.findByPedidoIdOrderByFechaAsc(id).stream()
                .map(CambioEstadoDTO::from)
                .toList();
    }

    /** Usa la regla del modelo: Pedido.cancelar(). */
    @Transactional
    public PedidoDTO cancelar(Long id, String motivo, String usuario, boolean soloPropios) {
        Pedido pedido = buscarConAcceso(id, usuario, soloPropios);
        pedido.cancelar(motivo, usuario);
        return PedidoDTO.from(pedido);
    }

    /**
     * Reprogramacion de un pedido (DESPACHANTE/ADMIN).
     *
     * El enunciado pide NO_ASIGNABLE -> RECIBIDO, pero EstadoPedido no tiene
     * NO_ASIGNABLE y el modelo no se puede tocar: se usa REPROGRAMADO, que el
     * modelo admite desde ASIGNADO y EN_CAMINO.
     */
    @Transactional
    public PedidoDTO reprogramar(Long id, String motivo, String usuario) {
        Pedido pedido = buscar(id);
        pedido.cambiarEstado(EstadoPedido.REPROGRAMADO, motivo, usuario);
        return PedidoDTO.from(pedido);
    }

    private Destinatario aDestinatario(DestinatarioDTO dto) {
        return new Destinatario(dto.nombre(), dto.telefono(), dto.direccion(),
                dto.latitud(), dto.longitud(), dto.referencias());
    }

    private Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.pedido(id));
    }

    /** 404 si no existe; 403 si es un CLIENTE y el pedido no es suyo (regla del modelo). */
    private Pedido buscarConAcceso(Long id, String keycloakUserId, boolean soloPropios) {
        Pedido pedido = buscar(id);
        if (soloPropios && !pedido.perteneceA(keycloakUserId)) {
            throw AccesoDenegadoException.pedidoAjeno(id);
        }
        return pedido;
    }

    // ------------------------------------------------------------------
    // Endpoints internos: despacho-service informa que ocurrio y este
    // servicio decide la transicion. Todas las operaciones son idempotentes.
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PedidoPendienteDTO> pendientes(Long centroId) {
        return pedidoRepository.findByEstadoAndCentroIdOrderByIdAsc(EstadoPedido.RECIBIDO, centroId).stream()
                .map(PedidoPendienteDTO::from)
                .toList();
    }

    /**
     * asignado = true  -> ASIGNADO.
     * asignado = false -> el enunciado pide NO_ASIGNABLE, que no existe en
     * EstadoPedido: el pedido queda en RECIBIDO (vuelve a la cola) y no se
     * registra ningun cambio de estado.
     */
    @Transactional
    public EstadoActualDTO registrarAsignacion(Long id, boolean asignado, String motivo, String servicio) {
        Pedido pedido = buscar(id);

        if (!asignado) {
            log.warn("Pedido {} no asignable (motivo: {}). Queda en {}: EstadoPedido no tiene NO_ASIGNABLE.",
                    id, motivo, pedido.getEstado());
            return EstadoActualDTO.from(pedido);
        }
        if (pedido.getEstado() == EstadoPedido.ASIGNADO) {
            return EstadoActualDTO.from(pedido);
        }
        pedido.cambiarEstado(EstadoPedido.ASIGNADO, motivo, servicio);
        return EstadoActualDTO.from(pedido);
    }

    /** El repartidor salio con el pedido: EN_CAMINO. */
    @Transactional
    public EstadoActualDTO registrarSalida(Long id, String servicio) {
        Pedido pedido = buscar(id);
        if (pedido.getEstado() == EstadoPedido.EN_CAMINO) {
            return EstadoActualDTO.from(pedido);
        }
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO, "Salida del centro de distribucion", servicio);
        return EstadoActualDTO.from(pedido);
    }

    /**
     * entregado = true  -> ENTREGADO.
     * entregado = false -> Pedido.registrarFallo(), que incrementa intentos y
     * reintenta (RECIBIDO) o devuelve (DEVUELTO) segun el maximo del modelo.
     */
    @Transactional
    public EstadoActualDTO registrarResultado(Long id, boolean entregado, String motivo, String servicio) {
        Pedido pedido = buscar(id);

        if (entregado) {
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                return EstadoActualDTO.from(pedido);
            }
            pedido.cambiarEstado(EstadoPedido.ENTREGADO, motivo, servicio);
            return EstadoActualDTO.from(pedido);
        }

        // Reintento del mismo aviso: el fallo ya se proceso y el pedido salio
        // de EN_CAMINO. Para volver a fallar tiene que haber una nueva salida.
        if (pedido.getEstado() != EstadoPedido.EN_CAMINO
                && pedido.getIntentos() > 0
                && (pedido.getEstado() == EstadoPedido.RECIBIDO || pedido.getEstado() == EstadoPedido.DEVUELTO)) {
            return EstadoActualDTO.from(pedido);
        }
        pedido.registrarFallo(motivo, servicio);
        return EstadoActualDTO.from(pedido);
    }
}
