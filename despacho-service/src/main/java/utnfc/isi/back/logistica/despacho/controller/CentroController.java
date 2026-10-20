package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.CentroRequest;
import utnfc.isi.back.logistica.despacho.dto.CentroResponse;
import utnfc.isi.back.logistica.despacho.service.CentroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Centros de distribución", description = "Datos maestros (escritura: ADMIN; lectura: ADMIN, DESPACHANTE)")
@RestController
@RequestMapping("/api/despacho/centros")
@RequiredArgsConstructor
public class CentroController {

    private final CentroService service;

    @Operation(summary = "Listar centros")
    @GetMapping
    public List<CentroResponse> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return service.listar(soloActivos);
    }

    @Operation(summary = "Obtener un centro")
    @GetMapping("/{id}")
    public CentroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un centro")
    @PostMapping
    public ResponseEntity<CentroResponse> crear(@Valid @RequestBody CentroRequest req) {
        CentroResponse creado = service.crear(req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri()).body(creado);
    }

    @Operation(summary = "Modificar un centro")
    @PutMapping("/{id}")
    public CentroResponse actualizar(@PathVariable Long id, @Valid @RequestBody CentroRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Desactivar un centro (baja lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
