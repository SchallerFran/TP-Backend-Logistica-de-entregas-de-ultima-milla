package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.CentroDistribucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CentroDistribucionRepository extends JpaRepository<CentroDistribucion, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<CentroDistribucion> findByActivoTrueOrderByNombre();
}
