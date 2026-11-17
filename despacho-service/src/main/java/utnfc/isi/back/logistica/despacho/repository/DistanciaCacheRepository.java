package utnfc.isi.back.logistica.despacho.repository;

import utnfc.isi.back.logistica.despacho.model.DistanciaCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DistanciaCacheRepository extends JpaRepository<DistanciaCache, Long> {

    Optional<DistanciaCache> findByOrigenLatAndOrigenLngAndDestinoLatAndDestinoLng(
            double origenLat, double origenLng, double destinoLat, double destinoLng);

    List<DistanciaCache> findByOrigenLatInAndDestinoLatIn(Collection<Double> origenLats,
                                                          Collection<Double> destinoLats);
}
