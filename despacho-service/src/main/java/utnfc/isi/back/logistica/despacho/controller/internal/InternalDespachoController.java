package utnfc.isi.back.logistica.despacho.controller.internal;

import utnfc.isi.back.logistica.despacho.dto.internal.CentroInternoDTO;
import utnfc.isi.back.logistica.despacho.service.CancelacionService;
import utnfc.isi.back.logistica.despacho.service.CentroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API interna para pedidos-service. Sólo accesible con token de cuenta de servicio (rol SERVICE) y
 * el gateway NO la enruta: no existe desde afuera del sistema.
 */
@Tag(name = "Interna (servicio a servicio)", description = "Sólo rol SERVICE. No expuesta por el gateway.")
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalDespachoController {

    private final CentroService centroService;
    private final CancelacionService cancelacionService;

    @Operation(summary = "Datos de un centro para validar el alta de un pedido (existencia, activo, cobertura)")
    @GetMapping("/centros/{id}")
    public CentroInternoDTO centro(@PathVariable Long id) {
        return centroService.obtenerInterno(id);
    }

    @Operation(summary = "Quitar el pedido de su ruta antes de cancelarlo",
            description = "204 si se quitó o no estaba en ninguna ruta; 409 si la ruta ya está en curso")
    @DeleteMapping("/paradas/pedido/{pedidoId}")
    public ResponseEntity<Void> quitarPedido(@PathVariable Long pedidoId) {
        cancelacionService.quitarPedido(pedidoId);
        return ResponseEntity.noContent().build();
    }
}
