package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.EstadoPlanificacion;
import utnfc.isi.back.logistica.despacho.dto.PlanificacionRequest;
import utnfc.isi.back.logistica.despacho.dto.PlanificacionResponse;
import utnfc.isi.back.logistica.despacho.service.PlanificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Planificación", description = "Asignación de pedidos a rutas (DESPACHANTE)")
@RestController
@RequestMapping("/api/despacho/planificaciones")
@RequiredArgsConstructor
public class PlanificacionController {

    private final PlanificacionService service;

    @Operation(summary = "Ejecutar la asignación de un centro para una fecha",
            description = "Arma rutas con los pedidos RECIBIDO del centro y los recursos libres, y las confirma "
                    + "con pedidos-service. Vuelve a planificar descarta las rutas BORRADOR anteriores.")
    @ApiResponse(responseCode = "200", description = "Rutas confirmadas (o sin rutas)")
    @ApiResponse(responseCode = "202", description = "Rutas en BORRADOR: pedidos-service no confirmó, reintentar")
    @ApiResponse(responseCode = "409", description = "Fecha pasada, centro inactivo o planificación concurrente")
    @ApiResponse(responseCode = "503", description = "pedidos-service no disponible para leer pendientes")
    @PostMapping
    public ResponseEntity<PlanificacionResponse> planificar(@Valid @RequestBody PlanificacionRequest req) {
        return responder(service.planificar(req));
    }

    @Operation(summary = "Reintentar la confirmación de las rutas en BORRADOR de un centro y fecha")
    @PostMapping("/confirmacion")
    public ResponseEntity<PlanificacionResponse> reintentarConfirmacion(@Valid @RequestBody PlanificacionRequest req) {
        return responder(service.reintentarConfirmacion(req));
    }

    private static ResponseEntity<PlanificacionResponse> responder(PlanificacionResponse r) {
        HttpStatus status = r.estado() == EstadoPlanificacion.PENDIENTE_CONFIRMACION ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(r);
    }
}
