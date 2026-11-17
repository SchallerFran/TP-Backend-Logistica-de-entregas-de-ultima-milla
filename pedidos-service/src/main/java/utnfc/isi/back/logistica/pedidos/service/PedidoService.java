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
import utnfc.isi.back.logistica.pedidos.dto.ConfirmacionAsignacionRequest;
import utnfc.isi.back.logistica.pedidos.dto.ConfirmacionAsignacionResponse;
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
        Cliente cliente = clienteRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(() -> new IllegalArgumentException("Debe registrar su perfil de cliente"));

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

        if (!pedido.ventanaValida()) {
            throw new IllegalArgumentException("La ventana de entrega es invalida: ventanaHasta debe ser posterior a ventanaDesde");
        }

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

    @Transactional
    public PedidoDTO cancelar(Long id, String motivo, String usuario, boolean soloPropios) {
        Pedido pedido = buscarConAcceso(id, usuario, soloPropios);
        if (pedido.getEstado() == EstadoPedido.ASIGNADO) {
            despachoClient.quitarDeRuta(id);
        }
        pedido.cancelar(motivo, usuario);
        return PedidoDTO.from(pedido);
    }

    @Transactional
    public PedidoDTO reprogramar(Long id, String motivo, String usuario) {
        Pedido pedido = buscar(id);
        pedido.reprogramar(motivo, usuario);
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

    private Pedido buscarConAcceso(Long id, String keycloakUserId, boolean soloPropios) {
        Pedido pedido = buscar(id);
        if (soloPropios && !pedido.perteneceA(keycloakUserId)) {
            throw AccesoDenegadoException.pedidoAjeno(id);
        }
        return pedido;
    }


    @Transactional(readOnly = true)
    public List<PedidoPendienteDTO> pendientes(Long centroId) {
        return pedidoRepository.findByEstadoAndCentroIdOrderByIdAsc(EstadoPedido.RECIBIDO, centroId).stream()
                .map(PedidoPendienteDTO::from)
                .toList();
    }

    @Transactional
    public ConfirmacionAsignacionResponse confirmarAsignacion(ConfirmacionAsignacionRequest request, String servicio) {
        List<Long> confirmados = new ArrayList<>();
        List<ConfirmacionAsignacionResponse.Rechazado> rechazados = new ArrayList<>();

        for (ConfirmacionAsignacionRequest.Ruta ruta : request.rutas()) {
            for (Long pedidoId : ruta.pedidoIds()) {
                Pedido pedido = pedidoRepository.findById(pedidoId).orElse(null);
                if (pedido == null) {
                    rechazados.add(new ConfirmacionAsignacionResponse.Rechazado(pedidoId, null, "El pedido no existe"));
                    continue;
                }
                if (pedido.getEstado() == EstadoPedido.RECIBIDO) {
                    pedido.cambiarEstado(EstadoPedido.ASIGNADO, "Asignado a la ruta " + ruta.rutaId(), servicio);
                }
                if (pedido.getEstado() == EstadoPedido.ASIGNADO) {
                    confirmados.add(pedidoId);
                } else {
                    rechazados.add(new ConfirmacionAsignacionResponse.Rechazado(
                            pedidoId, pedido.getEstado(), "El pedido esta en " + pedido.getEstado()));
                }
            }
        }

        for (ConfirmacionAsignacionRequest.NoAsignable noAsignable : request.noAsignables()) {
            String motivo = noAsignable.detalle() == null
                    ? noAsignable.codigo()
                    : noAsignable.codigo() + ": " + noAsignable.detalle();
            pedidoRepository.findById(noAsignable.pedidoId())
                    .filter(p -> p.getEstado() == EstadoPedido.RECIBIDO)
                    .ifPresentOrElse(
                            p -> p.cambiarEstado(EstadoPedido.NO_ASIGNABLE, motivo, servicio),
                            () -> log.warn("Pedido {} no asignable ignorado: no esta en RECIBIDO", noAsignable.pedidoId()));
        }
        return new ConfirmacionAsignacionResponse(confirmados, rechazados);
    }

    @Transactional
    public EstadoActualDTO registrarSalida(Long id, String servicio) {
        Pedido pedido = buscar(id);
        if (pedido.getEstado() == EstadoPedido.EN_CAMINO) {
            return EstadoActualDTO.from(pedido);
        }
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO, "Salida del centro de distribucion", servicio);
        return EstadoActualDTO.from(pedido);
    }

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

        if (pedido.getEstado() != EstadoPedido.EN_CAMINO
                && pedido.getIntentos() > 0
                && (pedido.getEstado() == EstadoPedido.RECIBIDO || pedido.getEstado() == EstadoPedido.DEVUELTO)) {
            return EstadoActualDTO.from(pedido);
        }
        pedido.registrarFallo(motivo, servicio);
        return EstadoActualDTO.from(pedido);
    }
}
