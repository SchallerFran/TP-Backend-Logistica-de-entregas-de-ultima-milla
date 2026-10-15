package utnfc.isi.back.logistica.despacho.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller de ejemplo: solo un GET de prueba para dejar visible la convencion.
 */
@RestController
@RequestMapping("/api/despacho")
@Tag(name = "Despacho")
public class EnvioController {

    @GetMapping("/ping")
    @PreAuthorize("hasAnyRole('DESPACHANTE', 'REPARTIDOR', 'ADMIN')")
    @Operation(summary = "Endpoint de prueba para validar seguridad y conectividad")
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok(Map.of("servicio", "despacho-service", "estado", "ok"));
    }
}
