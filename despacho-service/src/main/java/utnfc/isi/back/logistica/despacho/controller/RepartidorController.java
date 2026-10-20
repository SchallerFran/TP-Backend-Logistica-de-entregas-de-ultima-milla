package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.RepartidorRequest;
import utnfc.isi.back.logistica.despacho.dto.RepartidorResponse;
import utnfc.isi.back.logistica.despacho.service.RepartidorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Tag(name = "Repartidores", description = "Datos maestros (escritura: ADMIN; lectura: ADMIN, DESPACHANTE)")
@RestController
@RequestMapping("/api/despacho/repartidores")
@RequiredArgsConstructor
public class RepartidorController {

    private final RepartidorService service;

    @Operation(summary = "Listar repartidores, opcionalmente de un centro")
    @GetMapping
    public List<RepartidorResponse> listar(@RequestParam(required = false) Long centroId) {
        return service.listar(centroId);
    }

    @Operation(summary = "Obtener un repartidor")
    @GetMapping("/{id}")
    public RepartidorResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @Operation(summary = "Alta de repartidor (asociado a un usuario de Keycloak por su 'sub')")
    @PostMapping
    public ResponseEntity<RepartidorResponse> crear(@Valid @RequestBody RepartidorRequest req) {
        RepartidorResponse creado = service.crear(req);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri()).body(creado);
    }

    @Operation(summary = "Modificar repartidor (turno, centro, estado)")
    @PutMapping("/{id}")
    public RepartidorResponse actualizar(@PathVariable Long id, @Valid @RequestBody RepartidorRequest req) {
        return service.actualizar(id, req);
    }

    @Operation(summary = "Desactivar repartidor (baja lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
