package utnfc.isi.back.logistica.pedidos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import utnfc.isi.back.logistica.pedidos.dto.AsignacionRequest;
import utnfc.isi.back.logistica.pedidos.dto.EstadoActualDTO;
import utnfc.isi.back.logistica.pedidos.dto.PedidoPendienteDTO;
import utnfc.isi.back.logistica.pedidos.dto.ResultadoEntregaRequest;
import utnfc.isi.back.logistica.pedidos.service.PedidoService;

import java.util.List;

/**
 * API maquina a maquina para despacho-service. No se expone por el gateway.
 * despacho-service informa QUE ocurrio; este servicio decide la transicion.
 */
@RestController
@RequestMapping("/internal/pedidos")
@PreAuthorize("hasRole('SERVICE')")
@Tag(name = "Interno", description = "Solo para despacho-service (rol SERVICE). No pasa por el gateway")
public class PedidoInternoController {

    private static final String SERVICIO_DESCONOCIDO = "SERVICIO";

    private final PedidoService pedidoService;

    public PedidoInternoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/pendientes")
    @Operation(summary = "Pedidos en RECIBIDO de un centro, con los datos que necesita la asignacion")
    @ApiResponse(responseCode = "200", description = "Cola de pedidos pendientes")
    public List<PedidoPendienteDTO> pendientes(@RequestParam Long centroId) {
        return pedidoService.pendientes(centroId);
    }

    @PostMapping("/{id}/asignacion")
    @Operation(summary = "Resultado del intento de asignacion. Idempotente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado resultante"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "422", description = "Transicion no permitida")
    })
    public EstadoActualDTO asignacion(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable Long id,
                                      @Valid @RequestBody AsignacionRequest request) {
        return pedidoService.registrarAsignacion(id, request.asignado(), request.motivo(), servicio(jwt));
    }

    @PostMapping("/{id}/salida")
    @Operation(summary = "El pedido salio del centro: pasa a EN_CAMINO. Idempotente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado resultante"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "422", description = "Transicion no permitida")
    })
    public EstadoActualDTO salida(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return pedidoService.registrarSalida(id, servicio(jwt));
    }

    @PostMapping("/{id}/resultado")
    @Operation(summary = "Resultado del intento de entrega. Idempotente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado resultante"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "422", description = "Transicion no permitida")
    })
    public EstadoActualDTO resultado(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable Long id,
                                     @Valid @RequestBody ResultadoEntregaRequest request) {
        return pedidoService.registrarResultado(id, request.entregado(), request.motivo(), servicio(jwt));
    }

    /** realizadoPor de los cambios automaticos: el client id del token de servicio. */
    private String servicio(Jwt jwt) {
        String azp = jwt.getClaimAsString("azp");
        return azp != null ? azp : SERVICIO_DESCONOCIDO;
    }
}
