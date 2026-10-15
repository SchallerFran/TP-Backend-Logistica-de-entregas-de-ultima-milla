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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import utnfc.isi.back.logistica.pedidos.dto.ActualizarClienteRequest;
import utnfc.isi.back.logistica.pedidos.dto.ClienteDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearClienteRequest;
import utnfc.isi.back.logistica.pedidos.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Perfil del cliente y administracion de clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Crea el perfil del usuario autenticado (keycloakUserId = sub del token)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "409", description = "Ya tiene perfil o el CUIT ya esta registrado")
    })
    public ResponseEntity<ClienteDTO> crearPerfil(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CrearClienteRequest request,
                                                  UriComponentsBuilder uriBuilder) {
        ClienteDTO creado = clienteService.crearPerfil(jwt.getSubject(), request);
        return ResponseEntity
                .created(uriBuilder.path("/api/clientes/me").build().toUri())
                .body(creado);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Perfil del usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Todavia no creo su perfil")
    })
    public ClienteDTO miPerfil(@AuthenticationPrincipal Jwt jwt) {
        return clienteService.obtenerPerfil(jwt.getSubject());
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Actualiza email y telefono del perfil propio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos"),
            @ApiResponse(responseCode = "404", description = "Todavia no creo su perfil")
    })
    public ClienteDTO actualizarMiPerfil(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody ActualizarClienteRequest request) {
        return clienteService.actualizarPerfil(jwt.getSubject(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DESPACHANTE', 'ADMIN')")
    @Operation(summary = "Listado paginado de clientes")
    @ApiResponse(responseCode = "200", description = "Pagina de clientes")
    public Page<ClienteDTO> listar(Pageable pageable) {
        return clienteService.listar(pageable);
    }

    @PostMapping("/{id}/desactivacion")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desactiva un cliente: deja de poder crear pedidos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente desactivado"),
            @ApiResponse(responseCode = "404", description = "El cliente no existe")
    })
    public ClienteDTO desactivar(@PathVariable Long id) {
        return clienteService.desactivar(id);
    }
}
