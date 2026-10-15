package utnfc.isi.back.logistica.pedidos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utnfc.isi.back.logistica.pedidos.client.DespachoClient;
import utnfc.isi.back.logistica.pedidos.dto.CentroDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearPedidoRequest;
import utnfc.isi.back.logistica.pedidos.dto.DestinatarioDTO;
import utnfc.isi.back.logistica.pedidos.dto.EstadoActualDTO;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.model.Destinatario;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.model.PrioridadPedido;
import utnfc.isi.back.logistica.pedidos.repository.CambioEstadoRepository;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    private static final String SUB_CLIENTE = "sub-cliente-1";
    private static final String SERVICIO = "logistica-service";

    @Mock private PedidoRepository pedidoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CambioEstadoRepository cambioEstadoRepository;
    @Mock private DespachoClient despachoClient;

    @InjectMocks private PedidoService pedidoService;

    private Cliente cliente(boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setKeycloakUserId(SUB_CLIENTE);
        cliente.setRazonSocial("Distribuidora Sur SRL");
        cliente.setCuit("30-11111111-9");
        cliente.setEmail("ventas@sur.test");
        if (!activo) {
            cliente.desactivar();
        }
        return cliente;
    }

    private CrearPedidoRequest request(LocalDateTime desde, LocalDateTime hasta) {
        return new CrearPedidoRequest(
                7L,
                new DestinatarioDTO("Juan Perez", "351-5550000", "Av. Colon 1234", -31.4201, -64.1888, null),
                12.5, 0.3, PrioridadPedido.NORMAL, desde, hasta, false);
    }

    private CrearPedidoRequest requestValido() {
        return request(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(5));
    }

    private Pedido pedidoDe(Cliente cliente) {
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDestinatario(new Destinatario("Juan Perez", null, "Av. Colon 1234", -31.4201, -64.1888, null));
        pedido.setCentroId(7L);
        pedido.setPesoKg(12.5);
        pedido.setVolumenM3(0.3);
        pedido.setVentanaDesde(LocalDateTime.now().plusHours(1));
        pedido.setVentanaHasta(LocalDateTime.now().plusHours(5));
        return pedido;
    }

    @Test
    @DisplayName("Centro inexistente: 422 y el pedido no se guarda")
    void crearConCentroInexistente() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(despachoClient.obtenerCentro(7L)).thenThrow(new CentroInexistenteException(7L));

        assertThrows(CentroInexistenteException.class,
                () -> pedidoService.crear(SUB_CLIENTE, requestValido()));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("despacho-service caido: 503 y el pedido no se guarda")
    void crearConDespachoCaido() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(despachoClient.obtenerCentro(7L))
                .thenThrow(new ServicioNoDisponibleException("timeout"));

        assertThrows(ServicioNoDisponibleException.class,
                () -> pedidoService.crear(SUB_CLIENTE, requestValido()));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Centro inactivo: 422 por regla de negocio")
    void crearConCentroInactivo() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(despachoClient.obtenerCentro(7L))
                .thenReturn(new CentroDTO(7L, "Centro Sur", false, -31.4, -64.2));

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crear(SUB_CLIENTE, requestValido()));

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cliente desactivado: 422 sin consultar a despacho-service")
    void crearConClienteInactivo() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.of(cliente(false)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crear(SUB_CLIENTE, requestValido()));

        assertEquals("El cliente esta desactivado y no puede crear pedidos", ex.getMessage());
        verifyNoInteractions(despachoClient);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sin perfil de cliente: 422 sin consultar a despacho-service")
    void crearSinPerfil() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crear(SUB_CLIENTE, requestValido()));

        assertEquals("Debe registrar su perfil de cliente", ex.getMessage());
        verifyNoInteractions(despachoClient);
    }

    @Test
    @DisplayName("Ventana invalida: 422 antes de validar el centro")
    void crearConVentanaInvalida() {
        when(clienteRepository.findByKeycloakUserId(SUB_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        LocalDateTime ahora = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.crear(SUB_CLIENTE, request(ahora.plusHours(5), ahora.plusHours(1))));

        verifyNoInteractions(despachoClient);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un CLIENTE no puede cancelar el pedido de otro: 403")
    void cancelarPedidoAjeno() {
        Pedido ajeno = pedidoDe(cliente(true));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(ajeno));

        assertThrows(AccesoDenegadoException.class,
                () -> pedidoService.cancelar(5L, "me arrepenti", "sub-otro-cliente", true));

        assertEquals(EstadoPedido.RECIBIDO, ajeno.getEstado());
    }

    @Test
    @DisplayName("Un DESPACHANTE si puede cancelar el pedido de cualquiera")
    void cancelarComoDespachante() {
        Pedido pedido = pedidoDe(cliente(true));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        pedidoService.cancelar(5L, "sin stock", "sub-despachante", false);

        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
    }

    @Test
    @DisplayName("Pedido inexistente: 404")
    void obtenerInexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.obtener(99L, SUB_CLIENTE, true));
    }

    @Test
    @DisplayName("Asignacion repetida: idempotente, sin nuevo CambioEstado")
    void asignacionIdempotente() {
        Pedido pedido = pedidoDe(cliente(true));
        pedido.cambiarEstado(EstadoPedido.ASIGNADO, "primera asignacion", SERVICIO);
        int cambiosPrevios = pedido.getCambios().size();
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        EstadoActualDTO resultado = pedidoService.registrarAsignacion(5L, true, "reintento", SERVICIO);

        assertEquals(EstadoPedido.ASIGNADO, resultado.estado());
        assertEquals(cambiosPrevios, pedido.getCambios().size());
    }

    @Test
    @DisplayName("Primera asignacion: RECIBIDO -> ASIGNADO con su CambioEstado")
    void asignacionPrimeraVez() {
        Pedido pedido = pedidoDe(cliente(true));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        EstadoActualDTO resultado = pedidoService.registrarAsignacion(5L, true, "repartidor 3", SERVICIO);

        assertEquals(EstadoPedido.ASIGNADO, resultado.estado());
        assertEquals(1, pedido.getCambios().size());
        assertEquals(SERVICIO, pedido.getCambios().get(0).getRealizadoPor());
    }

    @Test
    @DisplayName("Fallo de entrega repetido: idempotente una vez procesado el reintento")
    void resultadoFallidoIdempotente() {
        Pedido pedido = pedidoDe(cliente(true));
        pedido.cambiarEstado(EstadoPedido.ASIGNADO, null, SERVICIO);
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO, null, SERVICIO);
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        pedidoService.registrarResultado(5L, false, "nadie en el domicilio", SERVICIO);
        int cambiosPrevios = pedido.getCambios().size();

        EstadoActualDTO reintento = pedidoService.registrarResultado(5L, false, "nadie en el domicilio", SERVICIO);

        assertEquals(EstadoPedido.RECIBIDO, reintento.estado());
        assertEquals(1, reintento.intentos());
        assertEquals(cambiosPrevios, pedido.getCambios().size());
    }

    @Test
    @DisplayName("Sin NO_ASIGNABLE en el modelo, un rechazo deja el pedido en RECIBIDO")
    void asignacionRechazada() {
        Pedido pedido = pedidoDe(cliente(true));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));

        EstadoActualDTO resultado = pedidoService.registrarAsignacion(5L, false, "sin repartidores", SERVICIO);

        assertEquals(EstadoPedido.RECIBIDO, resultado.estado());
        assertEquals(0, pedido.getCambios().size());
    }

    @Test
    @DisplayName("El historial se lee del repositorio de cambios")
    void historialUsaElRepositorio() {
        Pedido pedido = pedidoDe(cliente(true));
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedido));
        when(cambioEstadoRepository.findByPedidoIdOrderByFechaAsc(5L)).thenReturn(java.util.List.of());

        pedidoService.historial(5L, SUB_CLIENTE, true);

        verify(cambioEstadoRepository).findByPedidoIdOrderByFechaAsc(5L);
    }
}
