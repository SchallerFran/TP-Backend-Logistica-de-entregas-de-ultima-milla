package utnfc.isi.back.logistica.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utnfc.isi.back.logistica.pedidos.model.Cliente;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByKeycloakUserId(String keycloakUserId);

    boolean existsByCuit(String cuit);
}
