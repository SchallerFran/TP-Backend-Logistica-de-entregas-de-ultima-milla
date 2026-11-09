package utnfc.isi.back.logistica.pedidos.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.model.Destinatario;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * De punta a punta: transacción real (H2 en memoria) → listener JPA → evento AFTER_COMMIT → mail.
 * Gmail está simulado (JavaMailSender mock).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pedidos;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.mail.password=clave-test"
})
class NotificadorMailTest {

    @MockitoBean
    JavaMailSender mailSender;

    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    PedidoRepository pedidoRepository;
    @Autowired
    TransactionTemplate tx;

    @Test
    void mandaUnMailAlClientePorElAltaYPorCadaCambioDeEstado() {
        Long id = tx.execute(s -> pedidoRepository.save(nuevoPedido()).getId());
        tx.executeWithoutResult(s -> pedidoRepository.findById(id).orElseThrow()
                .cambiarEstado(EstadoPedido.ASIGNADO, "Asignado a la ruta 7", "test"));

        ArgumentCaptor<SimpleMailMessage> enviados = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000).times(2)).send(enviados.capture());

        List<String> asuntos = enviados.getAllValues().stream().map(SimpleMailMessage::getSubject).sorted().toList();
        assertEquals(List.of("Pedido #" + id + ": asignado a una ruta", "Pedido #" + id + ": recibido"), asuntos);
        assertArrayEquals(new String[]{"ventas@sur.test"}, enviados.getValue().getTo());
    }

    @Test
    void siLaTransaccionSeRevierteNoSaleNingunMail() {
        Long id = tx.execute(s -> pedidoRepository.save(nuevoPedido()).getId());
        verify(mailSender, timeout(5000).times(1)).send(any(SimpleMailMessage.class));
        clearInvocations(mailSender);

        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
            pedidoRepository.findById(id).orElseThrow().cambiarEstado(EstadoPedido.ASIGNADO, "x", "test");
            pedidoRepository.flush();
            throw new IllegalStateException("rollback");
        }));

        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));
    }

    private Pedido nuevoPedido() {
        Cliente cliente = new Cliente();
        cliente.setKeycloakUserId("sub-" + System.nanoTime());
        cliente.setRazonSocial("Distribuidora Sur SRL");
        cliente.setCuit(String.valueOf(System.nanoTime()).substring(0, 13));
        cliente.setEmail("ventas@sur.test");
        clienteRepository.save(cliente);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDestinatario(new Destinatario("Juan Perez", "351-5550000", "Av. Colon 1234", -31.4201, -64.1888, null));
        pedido.setCentroId(1L);
        pedido.setPesoKg(12.5);
        pedido.setVolumenM3(0.3);
        pedido.setVentanaDesde(LocalDateTime.now().plusHours(1));
        pedido.setVentanaHasta(LocalDateTime.now().plusHours(5));
        return pedido;
    }
}
