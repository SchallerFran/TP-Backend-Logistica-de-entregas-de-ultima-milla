package utnfc.isi.back.logistica.pedidos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;
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
    /** Casillas que reciben copia oculta de cada mail (las de prueba del TP). Vacío: sin copia. */
    private final String[] copia;

    public NotificadorMail(JavaMailSender mailSender, PedidoRepository pedidoRepository,
                           @Value("${spring.mail.username:}") String remitente,
                           @Value("${spring.mail.password:}") String password,
                           @Value("${notificaciones.mail.copia:}") String[] copia) {
        this.mailSender = mailSender;
        this.pedidoRepository = pedidoRepository;
        this.remitente = remitente;
        this.copia = copia;
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

        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            // multipart: va el HTML y, como alternativa para clientes de correo sin HTML, el texto plano
            MimeMessageHelper mail = new MimeMessageHelper(mensaje, true, "UTF-8");
            mail.setFrom(remitente);
            mail.setTo(cliente.getEmail());
            if (copia.length > 0) {
                mail.setBcc(copia);
            }
            mail.setSubject("Pedido #" + cambio.pedidoId() + ": " + titulo(cambio.nuevo()));
            mail.setText(cuerpo(cliente.getRazonSocial(), cambio), html(cliente.getRazonSocial(), cambio));
            mailSender.send(mensaje);
            log.info("Mail enviado a {} por el pedido {} ({})", cliente.getEmail(), cambio.pedidoId(), cambio.nuevo());
        } catch (MailException | MessagingException e) {
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

    /** Versión HTML del mail. Estilos en línea y tablas: es lo que respetan los clientes de correo. */
    static String html(String razonSocial, CambioRegistrado cambio) {
        String filas = (cambio.anterior() == null ? "" : fila("Estado anterior", cambio.anterior().name()))
                + fila("Estado actual", cambio.nuevo().name())
                + (cambio.motivo() == null || cambio.motivo().isBlank() ? "" : fila("Detalle", cambio.motivo()));
        return """
                <!DOCTYPE html>
                <html lang="es">
                <body style="margin:0;padding:0;background:#f3f4f6;font-family:Arial,Helvetica,sans-serif;color:#111827;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f3f4f6;padding:24px 12px;">
                  <tr><td align="center">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb;">
                      <tr><td style="background:#0f172a;padding:18px 28px;color:#ffffff;font-size:15px;font-weight:bold;letter-spacing:.3px;">
                        Logística de última milla
                      </td></tr>
                      <tr><td style="padding:28px 28px 8px 28px;">
                        <p style="margin:0 0 6px 0;font-size:15px;color:#4b5563;">Hola %s,</p>
                        <h1 style="margin:0 0 14px 0;font-size:24px;line-height:1.25;color:#0f172a;">Tu pedido #%d cambió de estado</h1>
                        <span style="display:inline-block;background:%s;color:#ffffff;font-size:14px;font-weight:bold;padding:7px 16px;border-radius:999px;">%s</span>
                      </td></tr>
                      <tr><td style="padding:20px 28px 28px 28px;">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-top:1px solid #e5e7eb;">
                %s
                        </table>
                      </td></tr>
                      <tr><td style="background:#f9fafb;border-top:1px solid #e5e7eb;padding:16px 28px;font-size:12px;line-height:1.5;color:#6b7280;">
                        Este es un aviso automático por el cambio de estado de tu pedido. No hace falta responderlo.
                      </td></tr>
                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(razonSocial), cambio.pedidoId(), color(cambio.nuevo()),
                capitalizar(titulo(cambio.nuevo())), filas);
    }

    /** Una fila "etiqueta: valor". El valor puede venir del usuario (motivo): siempre se escapa. */
    private static String fila(String etiqueta, String valor) {
        return """
                          <tr>
                            <td style="padding:12px 0;border-bottom:1px solid #e5e7eb;font-size:13px;color:#6b7280;width:38%%;vertical-align:top;">%s</td>
                            <td style="padding:12px 0;border-bottom:1px solid #e5e7eb;font-size:14px;color:#111827;font-weight:bold;">%s</td>
                          </tr>
                """.formatted(etiqueta, HtmlUtils.htmlEscape(valor));
    }

    private static String color(EstadoPedido estado) {
        return switch (estado) {
            case RECIBIDO, ASIGNADO, EN_CAMINO -> "#1d4ed8";
            case ENTREGADO -> "#15803d";
            case NO_ASIGNABLE, DEVUELTO -> "#b45309";
            case FALLIDO -> "#b91c1c";
            case CANCELADO -> "#4b5563";
        };
    }

    private static String capitalizar(String texto) {
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
