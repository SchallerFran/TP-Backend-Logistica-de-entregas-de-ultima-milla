package utnfc.isi.back.logistica.pedidos.service;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import utnfc.isi.back.logistica.pedidos.model.CambioEstadoListener.CambioRegistrado;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.model.Destinatario;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.model.Pedido;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        "spring.mail.password=clave-test",
        "notificaciones.mail.copia=uno@copia.test,dos@copia.test"
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

    @BeforeEach
    void elMockCreaMensajesReales() {
        when(mailSender.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
    }

    @Test
    void elHtmlEscapaLoQueVieneDelUsuario() {
        String html = NotificadorMail.html("Tom & <b>Jerry</b>",
                new CambioRegistrado(7L, EstadoPedido.EN_CAMINO, EstadoPedido.FALLIDO, "<script>x</script>"));

        assertTrue(html.contains("Tom &amp; &lt;b&gt;Jerry&lt;/b&gt;"));
        assertTrue(html.contains("&lt;script&gt;x&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("Tu pedido #7") && html.contains("EN_CAMINO") && html.contains("Entrega fallida"));
    }

    @Test
    void mandaUnMailAlClientePorElAltaYPorCadaCambioDeEstado() throws Exception {
        Long id = tx.execute(s -> pedidoRepository.save(nuevoPedido()).getId());
        tx.executeWithoutResult(s -> pedidoRepository.findById(id).orElseThrow()
                .cambiarEstado(EstadoPedido.ASIGNADO, "Asignado a la ruta 7", "test"));

        ArgumentCaptor<MimeMessage> enviados = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, timeout(5000).times(2)).send(enviados.capture());

        List<String> asuntos = new ArrayList<>();
        for (MimeMessage m : enviados.getAllValues()) {
            asuntos.add(m.getSubject());
        }
        assertEquals(List.of("Pedido #" + id + ": asignado a una ruta", "Pedido #" + id + ": recibido"),
                asuntos.stream().sorted().toList());
        MimeMessage ultimo = enviados.getValue();
        assertEquals("[ventas@sur.test]", List.of(ultimo.getRecipients(RecipientType.TO)).toString());
        assertEquals("[uno@copia.test, dos@copia.test]", List.of(ultimo.getRecipients(RecipientType.BCC)).toString());
    }

    @Test
    void siLaTransaccionSeRevierteNoSaleNingunMail() {
        Long id = tx.execute(s -> pedidoRepository.save(nuevoPedido()).getId());
        verify(mailSender, timeout(5000).times(1)).send(any(MimeMessage.class));
        clearInvocations(mailSender);

        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(s -> {
            pedidoRepository.findById(id).orElseThrow().cambiarEstado(EstadoPedido.ASIGNADO, "x", "test");
            pedidoRepository.flush();
            throw new IllegalStateException("rollback");
        }));

        verify(mailSender, after(1000).never()).send(any(MimeMessage.class));
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
