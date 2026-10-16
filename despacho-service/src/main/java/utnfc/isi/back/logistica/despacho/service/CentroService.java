package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.dto.internal.CentroInternoDTO;
import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.dto.CentroRequest;
import utnfc.isi.back.logistica.despacho.dto.CentroResponse;
import utnfc.isi.back.logistica.despacho.dto.DespachoMapper;
import utnfc.isi.back.logistica.despacho.model.CentroDistribucion;
import utnfc.isi.back.logistica.despacho.repository.CentroDistribucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CentroService {

    private final CentroDistribucionRepository repo;

    @Transactional(readOnly = true)
    public List<CentroResponse> listar(boolean soloActivos) {
        List<CentroDistribucion> centros = soloActivos ? repo.findByActivoTrueOrderByNombre() : repo.findAll();
        return centros.stream().map(DespachoMapper::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public CentroResponse obtener(Long id) {
        return DespachoMapper.aResponse(buscar(id));
    }

    /** Para pedidos-service (API interna). */
    @Transactional(readOnly = true)
    public CentroInternoDTO obtenerInterno(Long id) {
        return DespachoMapper.aInterno(buscar(id));
    }

    @Transactional
    public CentroResponse crear(CentroRequest req) {
        if (repo.existsByNombreIgnoreCase(req.nombre())) {
            throw new ReglaNegocioException("CENTRO_DUPLICADO", "Ya existe un centro con nombre " + req.nombre());
        }
        CentroDistribucion c = new CentroDistribucion();
        aplicar(c, req);
        return DespachoMapper.aResponse(repo.save(c));
    }

    @Transactional
    public CentroResponse actualizar(Long id, CentroRequest req) {
        CentroDistribucion c = buscar(id);
        if (repo.existsByNombreIgnoreCaseAndIdNot(req.nombre(), id)) {
            throw new ReglaNegocioException("CENTRO_DUPLICADO", "Ya existe un centro con nombre " + req.nombre());
        }
        aplicar(c, req);
        return DespachoMapper.aResponse(c);
    }

    /** Baja lógica: un centro con historial de rutas no se borra físicamente. */
    @Transactional
    public void desactivar(Long id) {
        buscar(id).setActivo(false);
    }

    public CentroDistribucion buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Centro", id));
    }

    private static void aplicar(CentroDistribucion c, CentroRequest req) {
        c.setNombre(req.nombre().trim());
        c.setDireccion(req.direccion().trim());
        c.setLatitud(req.latitud());
        c.setLongitud(req.longitud());
        c.setRadioCoberturaKm(req.radioCoberturaKm());
        if (req.activo() != null) {
            c.setActivo(req.activo());
        }
    }
}
