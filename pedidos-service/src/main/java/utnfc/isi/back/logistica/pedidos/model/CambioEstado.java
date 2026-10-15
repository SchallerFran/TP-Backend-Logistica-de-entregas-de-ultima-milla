package utnfc.isi.back.logistica.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro inmutable de un cambio de estado del pedido.
 * Se crea unicamente desde Pedido.cambiarEstado(): sin setters publicos.
 */
@Entity
@Table(name = "cambio_estado")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CambioEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    /** null en el registro de alta del pedido. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPedido estadoNuevo;

    @Column(length = 255)
    private String motivo;

    /** "sub" del usuario que lo realizo, o "SISTEMA" si fue automatico. */
    @Column(name = "realizado_por", nullable = false, length = 64)
    private String realizadoPor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public CambioEstado(Pedido pedido, EstadoPedido estadoAnterior, EstadoPedido estadoNuevo,
                        String motivo, String realizadoPor) {
        this.pedido = pedido;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
        this.realizadoPor = realizadoPor;
        this.fecha = LocalDateTime.now();
    }
}
