package utnfc.isi.back.logistica.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import utnfc.isi.back.logistica.pedidos.model.CambioEstadoListener.CambioRegistrado;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

/**
 * Avisa por mail (Gmail SMTP) al cliente dueño del pedido cada vez que cambia de estado.
 * <p>
 * Sale después del commit (AFTER_COMMIT): sólo se avisa lo que realmente quedó guardado.
 * Es asíncrono y nunca lanza excepciones: si Gmail falla o tarda, el pedido no se entera
 * y sólo queda el error en el log. Sin GMAIL_APP_PASSWORD no se envía nada.
 */
@Component
public class NotificadorMail {

    private static final Logger log = LoggerFactory.getLogger(NotificadorMail.class);

    private final JavaMailSender mailSender;
    private final PedidoRepository pedidoRepository;
    private final String remitente;
    private final boolean habilitado;

    public NotificadorMail(JavaMailSender mailSender, PedidoRepository pedidoRepository,
                           @Value("${spring.mail.username:}") String remitente,
                           @Value("${spring.mail.password:}") String password) {
        this.mailSender = mailSender;
        this.pedidoRepository = pedidoRepository;
        this.remitente = remitente;
        this.habilitado = !remitente.isBlank() && !password.isBlank();
    }

    // ponytail: un mail por cambio, sin cola ni reintentos. Gmail admite ~500 por día: alcanza para el TP.
    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void alCambiarEstado(CambioRegistrado cambio) {
        if (!habilitado) {
            log.debug("Mail del pedido {} no enviado: falta GMAIL_APP_PASSWORD", cambio.pedidoId());
            return;
        }
        Cliente cliente = pedidoRepository.findById(cambio.pedidoId()).orElseThrow().getCliente();
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            return;
        }

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(remitente);
        mail.setTo(cliente.getEmail());
        mail.setSubject("Pedido #" + cambio.pedidoId() + ": " + titulo(cambio.nuevo()));
        mail.setText(cuerpo(cliente.getRazonSocial(), cambio));
        try {
            mailSender.send(mail);
            log.info("Mail enviado a {} por el pedido {} ({})", cliente.getEmail(), cambio.pedidoId(), cambio.nuevo());
        } catch (MailException e) {
            log.warn("No se pudo enviar el mail del pedido {}: {}", cambio.pedidoId(), e.getMessage());
        }
    }

    static String titulo(EstadoPedido estado) {
        return switch (estado) {
            case RECIBIDO -> "recibido";
            case ASIGNADO -> "asignado a una ruta";
            case NO_ASIGNABLE -> "no se pudo asignar";
            case EN_CAMINO -> "en camino";
            case ENTREGADO -> "entregado";
            case FALLIDO -> "entrega fallida";
            case DEVUELTO -> "devuelto";
            case CANCELADO -> "cancelado";
        };
    }

    static String cuerpo(String razonSocial, CambioRegistrado cambio) {
        StringBuilder sb = new StringBuilder()
                .append("Hola ").append(razonSocial).append(",\n\n")
                .append("Tu pedido #").append(cambio.pedidoId()).append(" cambió de estado: ");
        if (cambio.anterior() != null) {
            sb.append(cambio.anterior()).append(" -> ");
        }
        sb.append(cambio.nuevo()).append(".\n");
        if (cambio.motivo() != null && !cambio.motivo().isBlank()) {
            sb.append("Detalle: ").append(cambio.motivo()).append("\n");
        }
        return sb.append("\nLogística de última milla").toString();
    }
}
