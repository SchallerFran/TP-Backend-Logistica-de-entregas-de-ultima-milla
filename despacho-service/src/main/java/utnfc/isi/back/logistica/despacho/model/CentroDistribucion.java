package utnfc.isi.back.logistica.despacho.model;

import utnfc.isi.back.logistica.commons.Coordenada;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Punto desde el que parten y al que vuelven las rutas. */
@Entity
@Table(name = "centro_distribucion",
        uniqueConstraints = @UniqueConstraint(name = "uk_centro_nombre", columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
public class CentroDistribucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false)
    private double latitud;

    @Column(nullable = false)
    private double longitud;

    @Column(name = "radio_cobertura_km", nullable = false)
    private double radioCoberturaKm;

    @Column(nullable = false)
    private boolean activo = true;

    public Coordenada coordenada() {
        return Coordenada.of(latitud, longitud);
    }
}
