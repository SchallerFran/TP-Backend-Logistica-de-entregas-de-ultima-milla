package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "distancia_cache", uniqueConstraints = @UniqueConstraint(
        name = "uk_distancia_par",
        columnNames = {"origen_lat", "origen_lng", "destino_lat", "destino_lng"}))
@Getter
@Setter
@NoArgsConstructor
public class DistanciaCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origen_lat", nullable = false)
    private double origenLat;

    @Column(name = "origen_lng", nullable = false)
    private double origenLng;

    @Column(name = "destino_lat", nullable = false)
    private double destinoLat;

    @Column(name = "destino_lng", nullable = false)
    private double destinoLng;

    @Column(nullable = false)
    private int metros;

    @Column(nullable = false)
    private int segundos;

    @Column(nullable = false)
    private LocalDateTime fecha;
}
