package utnfc.isi.back.logistica.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utnfc.isi.back.logistica.pedidos.model.CambioEstado;

import java.util.List;

@Repository
public interface CambioEstadoRepository extends JpaRepository<CambioEstado, Long> {

    List<CambioEstado> findByPedidoIdOrderByFechaAsc(Long pedidoId);
}
