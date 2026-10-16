package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Distancias ya consultadas a Google Maps. Reduce costo/cuota y sirve de respaldo si la API no
 * responde. Las coordenadas se guardan redondeadas a 4 decimales (Coordenada.redondeada()).
 */
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

    /** distance.value de Google. */
    @Column(nullable = false)
    private int metros;

    /** duration.value de Google. */
    @Column(nullable = false)
    private int segundos;

    /** Momento de la consulta, para descartar registros viejos. */
    @Column(nullable = false)
    private LocalDateTime fecha;
}
