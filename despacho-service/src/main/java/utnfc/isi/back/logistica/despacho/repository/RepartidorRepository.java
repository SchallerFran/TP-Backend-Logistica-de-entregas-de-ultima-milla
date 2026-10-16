package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.Repartidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RepartidorRepository extends JpaRepository<Repartidor, Long> {

    Optional<Repartidor> findByKeycloakUserId(String keycloakUserId);

    boolean existsByKeycloakUserId(String keycloakUserId);

    boolean existsByKeycloakUserIdAndIdNot(String keycloakUserId, Long id);

    List<Repartidor> findByCentroIdOrderByNombre(Long centroId);

    /** Repartidores activos del centro sin otra ruta en la fecha. */
    @Query("""
            select r from Repartidor r
            where r.centro.id = :centroId
              and r.activo = true
              and not exists (select ru from Ruta ru where ru.repartidor = r and ru.fecha = :fecha)
            order by r.turnoInicio, r.id
            """)
    List<Repartidor> findLibresParaFecha(@Param("centroId") Long centroId, @Param("fecha") LocalDate fecha);
}
