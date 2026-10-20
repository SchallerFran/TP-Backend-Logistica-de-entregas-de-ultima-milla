package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Recorrido de un día que combina un vehículo y un repartidor, con paradas ordenadas.
 * Un vehículo y un repartidor participan de una sola ruta por día (restricciones únicas).
 * El centro de la ruta es el del vehículo.
 */
@Entity
@Table(name = "ruta", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ruta_vehiculo_fecha", columnNames = {"vehiculo_id", "fecha"}),
        @UniqueConstraint(name = "uk_ruta_repartidor_fecha", columnNames = {"repartidor_id", "fecha"})
})
@Getter
@Setter
@NoArgsConstructor
public class Ruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRuta estado = EstadoRuta.BORRADOR;

    @Column(name = "km_total", nullable = false)
    private double kmTotal;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    @Column(name = "costo_total", nullable = false)
    private double costoTotal;

    /** true si alguna distancia salió del fallback (caché vencida / Haversine) y no de Google. */
    @Column(nullable = false)
    private boolean estimada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repartidor_id", nullable = false)
    private Repartidor repartidor;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Parada> paradas = new ArrayList<>();

    public void agregarParada(Parada parada) {
        parada.setRuta(this);
        parada.setOrden(paradas.size() + 1);
        paradas.add(parada);
    }

    /** Quita la parada del pedido (si está) y renumera el orden de las restantes. */
    public boolean quitarParadaDelPedido(Long pedidoId) {
        boolean quitada = paradas.removeIf(p -> p.getPedidoId().equals(pedidoId));
        if (quitada) {
            renumerar();
        }
        return quitada;
    }

    public void renumerar() {
        for (int i = 0; i < paradas.size(); i++) {
            paradas.get(i).setOrden(i + 1);
        }
    }

    public Optional<Parada> buscarParada(Long paradaId) {
        return paradas.stream().filter(p -> p.getId().equals(paradaId)).findFirst();
    }

    public boolean todasLasParadasConResultado() {
        return paradas.stream().noneMatch(p -> p.getEstado() == EstadoParada.PENDIENTE);
    }

    public CentroDistribucion getCentro() {
        return vehiculo.getCentro();
    }
}
