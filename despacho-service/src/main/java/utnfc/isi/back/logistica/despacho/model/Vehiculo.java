package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Recurso de transporte asociado a un centro. */
@Entity
@Table(name = "vehiculo",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehiculo_patente", columnNames = "patente"))
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String patente;

    @Column(name = "peso_max_kg", nullable = false)
    private double pesoMaxKg;

    @Column(name = "vol_max_m3", nullable = false)
    private double volMaxM3;

    @Column(name = "autonomia_km", nullable = false)
    private double autonomiaKm;

    @Column(name = "costo_por_km", nullable = false)
    private double costoPorKm;

    @Column(name = "tiene_frio", nullable = false)
    private boolean tieneFrio = false;

    @Column(nullable = false)
    private boolean disponible = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private CentroDistribucion centro;
}
