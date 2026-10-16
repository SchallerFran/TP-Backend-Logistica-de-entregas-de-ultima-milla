package utnfc.isi.back.logistica.despacho.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

/** Persona que conduce y realiza las entregas, asociada a un usuario de Keycloak. */
@Entity
@Table(name = "repartidor",
        uniqueConstraints = @UniqueConstraint(name = "uk_repartidor_keycloak", columnNames = "keycloak_user_id"))
@Getter
@Setter
@NoArgsConstructor
public class Repartidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    /** Claim "sub" del usuario en Keycloak. Permite que el repartidor vea sólo su ruta. */
    @Column(name = "keycloak_user_id", nullable = false, length = 64)
    private String keycloakUserId;

    @Column(name = "turno_inicio", nullable = false)
    private LocalTime turnoInicio;

    @Column(name = "turno_fin", nullable = false)
    private LocalTime turnoFin;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "centro_id", nullable = false)
    private CentroDistribucion centro;
}
