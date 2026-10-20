package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Outbox de notificaciones hacia pedidos-service (agregado al modelo original).
 * <p>
 * Problema que resuelve: iniciar una ruta o registrar una parada pasa en la calle y NO puede
 * depender de que pedidos-service esté arriba. Por eso despacho registra el hecho en su propia base
 * (en la misma transacción que cambia la ruta/parada) y lo envía después: inmediatamente si puede,
 * o con reintentos periódicos si pedidos no responde. Nada se pierde y el orden por pedido se respeta.
 */
@Entity
@Table(name = "notificacion_pedido", indexes = @Index(name = "ix_notif_estado", columnList = "estado, id"))
@Getter
@Setter
@NoArgsConstructor
public class NotificacionPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "ruta_id", nullable = false)
    private Long rutaId;

    /** Sólo para RESULTADO_ENTREGA. */
    private Boolean entregado;

    @Column(length = 255)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = 500)
    private String ultimoError;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "enviada_en")
    private LocalDateTime enviadaEn;

    public static NotificacionPedido enCamino(Long pedidoId, Long rutaId, LocalDateTime ahora) {
        NotificacionPedido n = new NotificacionPedido();
        n.tipo = TipoNotificacion.EN_CAMINO;
        n.pedidoId = pedidoId;
        n.rutaId = rutaId;
        n.creadaEn = ahora;
        return n;
    }

    public static NotificacionPedido resultado(Long pedidoId, Long rutaId, boolean entregado,
                                               String motivo, LocalDateTime ahora) {
        NotificacionPedido n = new NotificacionPedido();
        n.tipo = TipoNotificacion.RESULTADO_ENTREGA;
        n.pedidoId = pedidoId;
        n.rutaId = rutaId;
        n.entregado = entregado;
        n.motivo = motivo;
        n.creadaEn = ahora;
        return n;
    }
}
