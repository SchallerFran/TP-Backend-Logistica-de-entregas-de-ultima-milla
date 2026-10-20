package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.RutaResponse;
import utnfc.isi.back.logistica.despacho.model.EstadoRuta;
import utnfc.isi.back.logistica.despacho.service.EjecucionRutaService;
import utnfc.isi.back.logistica.despacho.service.PlanificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Rutas", description = "Supervisión de la jornada (DESPACHANTE; lectura también ADMIN)")
@RestController
@RequestMapping("/api/despacho/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final EjecucionRutaService ejecucion;
    private final PlanificacionService planificacion;

    @Operation(summary = "Rutas de una fecha (por defecto hoy), filtrables por centro y estado")
    @GetMapping
    public List<RutaResponse> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long centroId,
            @RequestParam(required = false) EstadoRuta estado) {
        return ejecucion.buscar(fecha, centroId, estado);
    }

    @Operation(summary = "Detalle de una ruta con sus paradas")
    @GetMapping("/{id}")
    public RutaResponse obtener(@PathVariable Long id) {
        return ejecucion.obtener(id);
    }

    @Operation(summary = "Descartar una ruta en BORRADOR")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> descartar(@PathVariable Long id) {
        planificacion.descartarBorrador(id);
        return ResponseEntity.noContent().build();
    }
}
