package utnfc.isi.back.logistica.despacho.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utnfc.isi.back.logistica.despacho.model.Envio;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {
}
