package utnfc.isi.back.logistica.pedidos.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de dominio puros: sin Spring, sin base de datos. */
class PedidoTest {

    private static final String USUARIO = "sub-despachante-1";

    private Pedido nuevoPedido() {
        Cliente cliente = new Cliente();
        cliente.setKeycloakUserId("sub-cliente-1");
        cliente.setRazonSocial("Distribuidora Sur SRL");
        cliente.setCuit("30-11111111-9");
        cliente.setEmail("ventas@sur.test");

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDestinatario(new Destinatario(
                "Juan Perez", "351-5550000", "Av. Colon 1234", -31.4201, -64.1888, null));
        pedido.setCentroId(1L);
        pedido.setPesoKg(12.5);
        pedido.setVolumenM3(0.3);
        pedido.setVentanaDesde(LocalDateTime.now().plusHours(1));
        pedido.setVentanaHasta(LocalDateTime.now().plusHours(5));
        return pedido;
    }

    /** Lleva el pedido hasta EN_CAMINO por transiciones validas. */
    private Pedido pedidoEnCamino() {
        Pedido pedido = nuevoPedido();
        pedido.cambiarEstado(EstadoPedido.ASIGNADO, "asignado a repartidor", USUARIO);
        pedido.cambiarEstado(EstadoPedido.EN_CAMINO, "salio del centro", USUARIO);
        return pedido;
    }

    @Test
    @DisplayName("Un pedido nuevo arranca en RECIBIDO, sin intentos y sin historial")
    void pedidoNuevo() {
        Pedido pedido = nuevoPedido();

        assertEquals(EstadoPedido.RECIBIDO, pedido.getEstado());
        assertEquals(PrioridadPedido.NORMAL, pedido.getPrioridad());
        assertEquals(0, pedido.getIntentos());
        assertFalse(pedido.isRequiereFrio());
        assertTrue(pedido.getCambios().isEmpty());
        assertFalse(pedido.esEstadoFinal());
    }

    @Nested
    @DisplayName("cambiarEstado")
    class CambiarEstado {

        @Test
        @DisplayName("Una transicion valida actualiza el estado y registra el cambio")
        void transicionValida() {
            Pedido pedido = nuevoPedido();

            pedido.cambiarEstado(EstadoPedido.ASIGNADO, "asignado a repartidor", USUARIO);

            assertEquals(EstadoPedido.ASIGNADO, pedido.getEstado());
            assertEquals(1, pedido.getCambios().size());

            CambioEstado cambio = pedido.getCambios().get(0);
            assertEquals(EstadoPedido.RECIBIDO, cambio.getEstadoAnterior());
            assertEquals(EstadoPedido.ASIGNADO, cambio.getEstadoNuevo());
            assertEquals("asignado a repartidor", cambio.getMotivo());
            assertEquals(USUARIO, cambio.getRealizadoPor());
            assertEquals(pedido, cambio.getPedido());
            assertNotNull(cambio.getFecha());
        }

        @Test
        @DisplayName("El camino feliz completo queda registrado en orden")
        void caminoFeliz() {
            Pedido pedido = nuevoPedido();

            pedido.cambiarEstado(EstadoPedido.ASIGNADO, null, USUARIO);
            pedido.cambiarEstado(EstadoPedido.EN_CAMINO, null, USUARIO);
            pedido.cambiarEstado(EstadoPedido.ENTREGADO, "firmado por el destinatario", USUARIO);

            assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
            assertTrue(pedido.esEstadoFinal());
            assertEquals(
                    List.of(EstadoPedido.ASIGNADO, EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO),
                    pedido.getCambios().stream().map(CambioEstado::getEstadoNuevo).toList());
        }

        @Test
        @DisplayName("Una transicion invalida lanza IllegalArgumentException y no toca el pedido")
        void transicionInvalida() {
            Pedido pedido = nuevoPedido();

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pedido.cambiarEstado(EstadoPedido.ENTREGADO, "nope", USUARIO));

            assertTrue(ex.getMessage().contains("RECIBIDO"));
            assertEquals(EstadoPedido.RECIBIDO, pedido.getEstado());
            assertTrue(pedido.getCambios().isEmpty());
        }

        @Test
        @DisplayName("DEVUELTO y CANCELADO no admiten ninguna transicion")
        void estadosFinalesSinSalida() {
            Pedido cancelado = nuevoPedido();
            cancelado.cancelar("se arrepintio el cliente", USUARIO);

            assertThrows(IllegalArgumentException.class,
                    () -> cancelado.cambiarEstado(EstadoPedido.ASIGNADO, null, USUARIO));
        }

        @Test
        @DisplayName("ENTREGADO conserva la salida a DEVUELTO para casos particulares")
        void entregadoPuedeDevolverse() {
            Pedido pedido = pedidoEnCamino();
            pedido.cambiarEstado(EstadoPedido.ENTREGADO, null, USUARIO);

            pedido.cambiarEstado(EstadoPedido.DEVUELTO, "el destinatario lo rechazo", USUARIO);

            assertEquals(EstadoPedido.DEVUELTO, pedido.getEstado());
            assertTrue(pedido.esEstadoFinal());
        }

        @Test
        @DisplayName("El usuario que realiza el cambio es obligatorio")
        void usuarioObligatorio() {
            Pedido pedido = nuevoPedido();

            assertThrows(IllegalArgumentException.class,
                    () -> pedido.cambiarEstado(EstadoPedido.ASIGNADO, "motivo", "  "));
        }
    }

    @Nested
    @DisplayName("cancelar")
    class Cancelar {

        @Test
        @DisplayName("Se puede cancelar en RECIBIDO")
        void desdeRecibido() {
            Pedido pedido = nuevoPedido();

            assertTrue(pedido.puedeCancelarse());
            pedido.cancelar("sin stock", USUARIO);

            assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
            assertTrue(pedido.esEstadoFinal());
            assertEquals(1, pedido.getCambios().size());
        }

        @Test
        @DisplayName("Se puede cancelar en ASIGNADO")
        void desdeAsignado() {
            Pedido pedido = nuevoPedido();
            pedido.cambiarEstado(EstadoPedido.ASIGNADO, null, USUARIO);

            assertTrue(pedido.puedeCancelarse());
            pedido.cancelar("el cliente se arrepintio", USUARIO);

            assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
        }

        @Test
        @DisplayName("Cancelar fuera de RECIBIDO/ASIGNADO lanza IllegalArgumentException")
        void fueraDeEstado() {
            Pedido pedido = pedidoEnCamino();

            assertFalse(pedido.puedeCancelarse());
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> pedido.cancelar("tarde", USUARIO));

            assertTrue(ex.getMessage().contains("EN_CAMINO"));
            assertEquals(EstadoPedido.EN_CAMINO, pedido.getEstado());
        }
    }

    @Nested
    @DisplayName("registrarFallo")
    class RegistrarFallo {

        @Test
        @DisplayName("Primer fallo: pasa por FALLIDO y el sistema lo reintenta (RECIBIDO)")
        void primerIntento() {
            Pedido pedido = pedidoEnCamino();
            int cambiosPrevios = pedido.getCambios().size();

            pedido.registrarFallo("nadie en el domicilio", USUARIO);

            assertEquals(1, pedido.getIntentos());
            assertEquals(EstadoPedido.RECIBIDO, pedido.getEstado());
            assertFalse(pedido.esEstadoFinal());

            List<CambioEstado> nuevos = pedido.getCambios().subList(cambiosPrevios, pedido.getCambios().size());
            assertEquals(2, nuevos.size());
            assertEquals(EstadoPedido.FALLIDO, nuevos.get(0).getEstadoNuevo());
            assertEquals(USUARIO, nuevos.get(0).getRealizadoPor());
            assertEquals(EstadoPedido.RECIBIDO, nuevos.get(1).getEstadoNuevo());
            assertEquals(Pedido.USUARIO_SISTEMA, nuevos.get(1).getRealizadoPor());
        }

        @Test
        @DisplayName("Segundo fallo: el sistema lo devuelve (DEVUELTO)")
        void segundoIntento() {
            Pedido pedido = pedidoEnCamino();
            pedido.registrarFallo("nadie en el domicilio", USUARIO);

            // segundo reparto
            pedido.cambiarEstado(EstadoPedido.ASIGNADO, null, USUARIO);
            pedido.cambiarEstado(EstadoPedido.EN_CAMINO, null, USUARIO);
            int cambiosPrevios = pedido.getCambios().size();

            pedido.registrarFallo("domicilio inexistente", USUARIO);

            assertEquals(2, pedido.getIntentos());
            assertEquals(EstadoPedido.DEVUELTO, pedido.getEstado());
            assertTrue(pedido.esEstadoFinal());

            List<CambioEstado> nuevos = pedido.getCambios().subList(cambiosPrevios, pedido.getCambios().size());
            assertEquals(2, nuevos.size());
            assertEquals(EstadoPedido.FALLIDO, nuevos.get(0).getEstadoNuevo());
            assertEquals(EstadoPedido.DEVUELTO, nuevos.get(1).getEstadoNuevo());
            assertEquals(Pedido.USUARIO_SISTEMA, nuevos.get(1).getRealizadoPor());
        }

        @Test
        @DisplayName("Solo se puede registrar un fallo desde EN_CAMINO")
        void fueraDeEnCamino() {
            Pedido pedido = nuevoPedido();

            assertThrows(IllegalArgumentException.class,
                    () -> pedido.registrarFallo("nadie atendio", USUARIO));
            assertEquals(0, pedido.getIntentos());
            assertEquals(EstadoPedido.RECIBIDO, pedido.getEstado());
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultas {

        @Test
        @DisplayName("El historial expuesto no se puede modificar desde afuera")
        void historialInmutable() {
            Pedido pedido = nuevoPedido();
            pedido.cambiarEstado(EstadoPedido.ASIGNADO, null, USUARIO);

            assertThrows(UnsupportedOperationException.class, () -> pedido.getCambios().clear());
        }

        @Test
        @DisplayName("perteneceA compara contra el sub del cliente")
        void perteneceA() {
            Pedido pedido = nuevoPedido();

            assertTrue(pedido.perteneceA("sub-cliente-1"));
            assertFalse(pedido.perteneceA("sub-otro"));
            assertFalse(pedido.perteneceA(null));
        }

        @Test
        @DisplayName("ventanaValida exige que el fin sea posterior al inicio")
        void ventanaValida() {
            Pedido pedido = nuevoPedido();
            assertTrue(pedido.ventanaValida());

            pedido.setVentanaHasta(pedido.getVentanaDesde().minusMinutes(1));
            assertFalse(pedido.ventanaValida());
        }

        @Test
        @DisplayName("El destinatario expone su Coordenada del JAR comun")
        void coordenadaDelDestinatario() {
            Pedido pedido = nuevoPedido();

            assertEquals(-31.4201, pedido.getDestinatario().coordenada().latitud());
            assertEquals(-64.1888, pedido.getDestinatario().coordenada().longitud());
        }
    }
}
