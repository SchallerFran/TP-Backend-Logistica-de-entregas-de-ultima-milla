package utnfc.isi.back.logistica.pedidos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import utnfc.isi.back.logistica.pedidos.dto.CambioEstadoDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearPedidoRequest;
import utnfc.isi.back.logistica.pedidos.dto.MotivoRequest;
import utnfc.isi.back.logistica.pedidos.dto.PedidoDTO;
import utnfc.isi.back.logistica.pedidos.model.EstadoPedido;
import utnfc.isi.back.logistica.pedidos.service.PedidoService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Alta y seguimiento de pedidos de ultima milla")
public class PedidoController {

    /** Roles que ven y operan sobre los pedidos de cualquier cliente. */
    private static final Set<String> ROLES_GLOBALES = Set.of("ROLE_DESPACHANTE", "ROLE_ADMIN");

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crea un pedido para el cliente autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado (header Location)"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "422", description = "Sin perfil, cliente desactivado, ventana invalida o centro inexistente/inactivo"),
            @ApiResponse(responseCode = "503", description = "despacho-service no responde: el pedido no se crea")
    })
    public ResponseEntity<PedidoDTO> crear(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody CrearPedidoRequest request,
                                           UriComponentsBuilder uriBuilder) {
        PedidoDTO creado = pedidoService.crear(jwt.getSubject(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/pedidos/{id}").buildAndExpand(creado.id()).toUri())
                .body(creado);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Lista pedidos: el CLIENTE solo los propios, DESPACHANTE y ADMIN todos")
    @ApiResponse(responseCode = "200", description = "Pagina de pedidos")
    public Page<PedidoDTO> listar(@AuthenticationPrincipal Jwt jwt,
                                  Authentication authentication,
                                  @RequestParam(required = false) EstadoPedido estado,
                                  @RequestParam(required = false) Long centroId,
                                  Pageable pageable) {
        return pedidoService.listar(jwt.getSubject(), soloPropios(authentication), estado, centroId, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Detalle de un pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "403", description = "El pedido es de otro cliente"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe")
    })
    public PedidoDTO obtener(@AuthenticationPrincipal Jwt jwt,
                             Authentication authentication,
                             @PathVariable Long id) {
        return pedidoService.obtener(id, jwt.getSubject(), soloPropios(authentication));
    }

    @GetMapping("/{id}/cambios")
    @PreAuthorize("hasAnyRole('CLIENTE', 'DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Historial de cambios de estado, ordenado por fecha")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial del pedido"),
            @ApiResponse(responseCode = "403", description = "El pedido es de otro cliente"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe")
    })
    public List<CambioEstadoDTO> cambios(@AuthenticationPrincipal Jwt jwt,
                                         Authentication authentication,
                                         @PathVariable Long id) {
        return pedidoService.historial(id, jwt.getSubject(), soloPropios(authentication));
    }

    @PostMapping("/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('CLIENTE', 'DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Cancela el pedido (solo en RECIBIDO o ASIGNADO)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido cancelado"),
            @ApiResponse(responseCode = "403", description = "El pedido es de otro cliente"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "422", description = "El pedido no puede cancelarse en su estado actual")
    })
    public PedidoDTO cancelar(@AuthenticationPrincipal Jwt jwt,
                              Authentication authentication,
                              @PathVariable Long id,
                              @Valid @RequestBody MotivoRequest request) {
        return pedidoService.cancelar(id, request.motivo(), jwt.getSubject(), soloPropios(authentication));
    }

    @PostMapping("/{id}/reprogramacion")
    @PreAuthorize("hasAnyRole('DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Reprograma el pedido (REPROGRAMADO; valido desde ASIGNADO o EN_CAMINO)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido reprogramado"),
            @ApiResponse(responseCode = "404", description = "El pedido no existe"),
            @ApiResponse(responseCode = "422", description = "Transicion no permitida desde el estado actual")
    })
    public PedidoDTO reprogramar(@AuthenticationPrincipal Jwt jwt,
                                 @PathVariable Long id,
                                 @Valid @RequestBody MotivoRequest request) {
        return pedidoService.reprogramar(id, request.motivo(), jwt.getSubject());
    }

    /** true cuando el usuario solo puede ver y operar sus propios pedidos. */
    private boolean soloPropios(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(ROLES_GLOBALES::contains);
    }
}
