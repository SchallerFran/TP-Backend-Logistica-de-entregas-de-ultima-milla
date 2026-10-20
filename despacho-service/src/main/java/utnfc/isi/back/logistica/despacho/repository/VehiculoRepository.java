package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    boolean existsByPatenteIgnoreCase(String patente);

    boolean existsByPatenteIgnoreCaseAndIdNot(String patente, Long id);

    List<Vehiculo> findByCentroIdOrderByPatente(Long centroId);

    /**
     * Vehículos del centro que pueden usarse en la fecha: disponibles y sin otra ruta ese día.
     * Las rutas en BORRADOR se borran antes de planificar, así que no bloquean.
     */
    @Query("""
            select v from Vehiculo v
            where v.centro.id = :centroId
              and v.disponible = true
              and not exists (select r from Ruta r where r.vehiculo = v and r.fecha = :fecha)
            order by v.costoPorKm, v.id
            """)
    List<Vehiculo> findLibresParaFecha(@Param("centroId") Long centroId, @Param("fecha") LocalDate fecha);
}
