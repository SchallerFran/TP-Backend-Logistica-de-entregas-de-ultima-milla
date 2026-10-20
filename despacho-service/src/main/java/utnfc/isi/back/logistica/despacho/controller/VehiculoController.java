package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.VehiculoRequest;
import utnfc.isi.back.logistica.despacho.dto.VehiculoResponse;
import utnfc.isi.back.logistica.despacho.service.VehiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Vehículos", description = "Flota (escritura: ADMIN; lectura: ADMIN, DESPACHANTE)")
@RestController
@RequestMapping("/api/despacho/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final VehiculoService service;

    @Operation(summary = "Listar vehículos, opcionalmente de un centro")
    @GetMapping
    public List<VehiculoResponse> listar(@RequestParam(required = false) Long centroId) {
        return service.listar(centroId);
    }

    @Operation(summary = "Obtener un vehículo")
    @GetMapping("/{id}")
    public VehiculoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Alta de vehículo")
    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(@Valid @RequestBody VehiculoRequest req) {
        VehiculoResponse creado = service.crear(req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri()).body(creado);
    }

    @Operation(summary = "Modificar vehículo (capacidades, costo por km, disponibilidad)")
    @PutMapping("/{id}")
    public VehiculoResponse actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Baja lógica: deja de estar disponible")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> baja(@PathVariable Long id) {
        service.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }
}
