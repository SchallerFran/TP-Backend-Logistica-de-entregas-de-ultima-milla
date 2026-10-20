package utnfc.isi.back.logistica.despacho.service;

import utnfc.isi.back.logistica.despacho.exception.RecursoNoEncontradoException;
import utnfc.isi.back.logistica.despacho.exception.ReglaNegocioException;
import utnfc.isi.back.logistica.despacho.dto.DespachoMapper;
import utnfc.isi.back.logistica.despacho.dto.VehiculoRequest;
import utnfc.isi.back.logistica.despacho.dto.VehiculoResponse;
import utnfc.isi.back.logistica.despacho.model.Vehiculo;
import utnfc.isi.back.logistica.despacho.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository repo;
    private final CentroService centroService;

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(Long centroId) {
        List<Vehiculo> vehiculos = centroId == null ? repo.findAll() : repo.findByCentroIdOrderByPatente(centroId);
        return vehiculos.stream().map(DespachoMapper::aResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponse obtener(Long id) {
        return DespachoMapper.aResponse(buscar(id));
    }

    @Transactional
    public VehiculoResponse crear(VehiculoRequest req) {
        String patente = normalizar(req.patente());
        if (repo.existsByPatenteIgnoreCase(patente)) {
            throw new ReglaNegocioException("PATENTE_DUPLICADA", "Ya existe un vehículo con patente " + patente);
        }
        Vehiculo v = new Vehiculo();
        aplicar(v, req, patente);
        return DespachoMapper.aResponse(repo.save(v));
    }

    @Transactional
    public VehiculoResponse actualizar(Long id, VehiculoRequest req) {
        Vehiculo v = buscar(id);
        String patente = normalizar(req.patente());
        if (repo.existsByPatenteIgnoreCaseAndIdNot(patente, id)) {
            throw new ReglaNegocioException("PATENTE_DUPLICADA", "Ya existe un vehículo con patente " + patente);
        }
        aplicar(v, req, patente);
        return DespachoMapper.aResponse(v);
    }

    /** Baja lógica: deja de estar disponible para nuevas planificaciones. */
    @Transactional
    public void darDeBaja(Long id) {
        buscar(id).setDisponible(false);
    }

    private Vehiculo buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> RecursoNoEncontradoException.de("Vehículo", id));
    }

    private void aplicar(Vehiculo v, VehiculoRequest req, String patente) {
        v.setPatente(patente);
        v.setPesoMaxKg(req.pesoMaxKg());
        v.setVolMaxM3(req.volMaxM3());
        v.setAutonomiaKm(req.autonomiaKm());
        v.setCostoPorKm(req.costoPorKm());
        if (req.tieneFrio() != null) {
            v.setTieneFrio(req.tieneFrio());
        }
        if (req.disponible() != null) {
            v.setDisponible(req.disponible());
        }
        v.setCentro(centroService.buscar(req.centroId()));
    }

    private static String normalizar(String patente) {
        return patente.replace(" ", "").toUpperCase();
    }
}
