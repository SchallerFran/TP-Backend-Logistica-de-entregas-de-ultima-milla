package utnfc.isi.back.logistica.despacho.model;

import utnfc.isi.back.logistica.commons.Coordenada;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entrega de un pedido dentro de una ruta. Guarda una COPIA de los datos del pedido al momento de
 * asignar, para poder ejecutar la ruta aunque pedidos-service no responda.
 */
@Entity
@Table(name = "parada", uniqueConstraints = {
        @UniqueConstraint(name = "uk_parada_ruta_orden", columnNames = {"ruta_id", "orden"}),
        @UniqueConstraint(name = "uk_parada_ruta_pedido", columnNames = {"ruta_id", "pedido_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ruta_id", nullable = false)
    private Ruta ruta;

    @Column(nullable = false)
    private int orden;

    /** Referencia lógica al pedido de pedidos-service (sin FK). */
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(name = "peso_kg", nullable = false)
    private double pesoKg;

    @Column(name = "volumen_m3", nullable = false)
    private double volumenM3;

    @Column(name = "ventana_desde", nullable = false)
    private LocalDateTime ventanaDesde;

    @Column(name = "ventana_hasta", nullable = false)
    private LocalDateTime ventanaHasta;

    @Column(name = "eta_estimada")
    private LocalDateTime etaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoParada estado = EstadoParada.PENDIENTE;

    @Column(name = "motivo_fallo", length = 255)
    private String motivoFallo;

    public Coordenada coordenada() {
        return Coordenada.of(latitud, longitud);
    }
}
