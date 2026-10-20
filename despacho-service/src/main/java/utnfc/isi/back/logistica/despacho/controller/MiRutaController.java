package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.ResultadoParadaRequest;
import utnfc.isi.back.logistica.despacho.dto.RutaResponse;
import utnfc.isi.back.logistica.despacho.service.EjecucionRutaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Endpoints del repartidor. La identidad sale SIEMPRE del token (claim "sub"), nunca de un parámetro:
 * así un repartidor no puede operar la ruta de otro cambiando un id en la URL.
 */
@Tag(name = "Mi ruta", description = "Ejecución de la ruta por el repartidor autenticado (REPARTIDOR)")
@RestController
@RequestMapping("/api/despacho/mi-ruta")
@RequiredArgsConstructor
public class MiRutaController {

    private final EjecucionRutaService service;

    @Operation(summary = "Mi ruta del día (o de la fecha indicada)")
    @GetMapping
    public RutaResponse miRuta(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                               LocalDate fecha) {
        return service.miRuta(jwt.getSubject(), fecha);
    }

    @Operation(summary = "Iniciar la ruta: los pedidos pasan a EN_CAMINO")
    @PostMapping("/{rutaId}/inicio")
    public RutaResponse iniciar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long rutaId) {
        return service.iniciar(jwt.getSubject(), rutaId);
    }

    @Operation(summary = "Registrar el resultado de una parada (ENTREGADA o FALLIDA con motivo)")
    @PostMapping("/{rutaId}/paradas/{paradaId}/resultado")
    public RutaResponse registrarResultado(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable Long rutaId,
                                           @PathVariable Long paradaId,
                                           @Valid @RequestBody ResultadoParadaRequest req) {
        return service.registrarResultado(jwt.getSubject(), rutaId, paradaId, req);
    }
}
