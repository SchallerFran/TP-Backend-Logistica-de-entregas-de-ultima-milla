package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.dto.DespachoMapper;
import utnfc.isi.back.logistica.despacho.dto.RepartidorRequest;
import utnfc.isi.back.logistica.despacho.dto.RepartidorResponse;
import utnfc.isi.back.logistica.despacho.model.Repartidor;
import utnfc.isi.back.logistica.despacho.repository.RepartidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RepartidorService {

    private final RepartidorRepository repo;
    private final CentroService centroService;

    @Transactional(readOnly = true)
    public List<RepartidorResponse> listar(Long centroId) {
        List<Repartidor> lista = centroId == null ? repo.findAll() : repo.findByCentroIdOrderByNombre(centroId);
        return lista.stream().map(DespachoMapper::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public RepartidorResponse obtener(Long id) {
        return DespachoMapper.aResponse(buscar(id));
    }

    @Transactional
    public RepartidorResponse crear(RepartidorRequest req) {
        if (repo.existsByKeycloakUserId(req.keycloakUserId())) {
            throw new ReglaNegocioException("USUARIO_YA_ASOCIADO",
                    "El usuario de Keycloak ya está asociado a otro repartidor");
        }
        Repartidor r = new Repartidor();
        aplicar(r, req);
        return DespachoMapper.aResponse(repo.save(r));
    }

    @Transactional
    public RepartidorResponse actualizar(Long id, RepartidorRequest req) {
        Repartidor r = buscar(id);
        if (repo.existsByKeycloakUserIdAndIdNot(req.keycloakUserId(), id)) {
            throw new ReglaNegocioException("USUARIO_YA_ASOCIADO",
                    "El usuario de Keycloak ya está asociado a otro repartidor");
        }
        aplicar(r, req);
        return DespachoMapper.aResponse(r);
    }

    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    private Repartidor buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Repartidor", id));
    }

    private void aplicar(Repartidor r, RepartidorRequest req) {
        r.setNombre(req.nombre().trim());
        r.setKeycloakUserId(req.keycloakUserId().trim());
        r.setTurnoInicio(req.turnoInicio());
        r.setTurnoFin(req.turnoFin());
        if (req.activo() != null) {
            r.setActivo(req.activo());
        }
        r.setCentro(centroService.buscar(req.centroId()));
    }
}
