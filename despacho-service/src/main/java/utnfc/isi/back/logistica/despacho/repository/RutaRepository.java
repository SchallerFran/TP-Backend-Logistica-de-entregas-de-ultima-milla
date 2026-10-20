package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.EstadoRuta;
import utnfc.isi.back.logistica.despacho.model.Ruta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.centro", "repartidor"})
    @Query("select r from Ruta r where r.id = :id")
    Optional<Ruta> findConRecursos(@Param("id") Long id);

    /** Consulta del despachante; centroId y estado son opcionales. */
    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.centro", "repartidor"})
    @Query("""
            select r from Ruta r
            where r.fecha = :fecha
              and (:centroId is null or r.vehiculo.centro.id = :centroId)
              and (:estado is null or r.estado = :estado)
            order by r.id
            """)
    List<Ruta> buscar(@Param("fecha") LocalDate fecha,
                      @Param("centroId") Long centroId,
                      @Param("estado") EstadoRuta estado);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.centro", "repartidor"})
    @Query("""
            select r from Ruta r
            where r.vehiculo.centro.id = :centroId and r.fecha = :fecha and r.estado = :estado
            order by r.id
            """)
    List<Ruta> findByCentroFechaEstado(@Param("centroId") Long centroId,
                                       @Param("fecha") LocalDate fecha,
                                       @Param("estado") EstadoRuta estado);

    @EntityGraph(attributePaths = {"vehiculo", "vehiculo.centro", "repartidor"})
    @Query("""
            select r from Ruta r
            where r.repartidor.keycloakUserId = :keycloakUserId and r.fecha = :fecha
            """)
    Optional<Ruta> findDelRepartidor(@Param("keycloakUserId") String keycloakUserId,
                                     @Param("fecha") LocalDate fecha);

    /** Ruta no finalizada que contiene al pedido (para la cancelación desde pedidos-service). */
    @Query("""
            select distinct r from Ruta r join r.paradas p
            where p.pedidoId = :pedidoId and r.estado in :estados
            """)
    List<Ruta> findConPedidoEnEstados(@Param("pedidoId") Long pedidoId,
                                      @Param("estados") Collection<EstadoRuta> estados);
}
