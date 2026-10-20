package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.dto.NotificacionResponse;
import utnfc.isi.back.logistica.despacho.model.EstadoNotificacion;
import utnfc.isi.back.logistica.despacho.service.NotificacionPedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Notificaciones a pedidos", description = "Supervisión del outbox hacia pedidos-service (DESPACHANTE, ADMIN)")
@RestController
@RequestMapping("/api/despacho/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionPedidoService service;

    @Operation(summary = "Notificaciones por estado (PENDIENTE: pedidos-service no respondió; ERROR: requieren revisión)")
    @GetMapping
    public List<NotificacionResponse> listar(@RequestParam(defaultValue = "ERROR") EstadoNotificacion estado) {
        return service.listar(estado);
    }

    @Operation(summary = "Volver a encolar una notificación en ERROR")
    @PostMapping("/{id}/reintento")
    public NotificacionResponse reencolar(@PathVariable Long id) {
        NotificacionResponse r = service.reencolar(id);
        service.procesarPendientes();
        return r;
    }
}
