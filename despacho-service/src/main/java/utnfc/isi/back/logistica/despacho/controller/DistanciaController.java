package utnfc.isi.back.logistica.despacho.controller;

import utnfc.isi.back.logistica.despacho.geo.Coordenada;
import utnfc.isi.back.logistica.despacho.dto.DistanciaResponse;
import utnfc.isi.back.logistica.despacho.service.DistanciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Distancias", description = "Consulta de distancia/tiempo por calle (DESPACHANTE, ADMIN)")
@RestController
@RequestMapping("/api/despacho/distancias")
@RequiredArgsConstructor
public class DistanciaController {

    private final DistanciaService service;

    @Operation(summary = "Distancia y tiempo entre dos coordenadas",
            description = "Indica la fuente del dato: CACHE, GOOGLE, CACHE_VENCIDA o ESTIMADA (fallback Haversine)")
    @GetMapping
    public DistanciaResponse consultar(
            @Parameter(example = "-31.4135,-64.1810") @RequestParam String origen,
            @Parameter(example = "-31.4400,-64.2200") @RequestParam String destino) {
        Coordenada o = Coordenada.parse(origen);
        Coordenada d = Coordenada.parse(destino);
        var r = service.entre(o, d);
        return new DistanciaResponse(o.comoLatLng(), d.comoLatLng(), Math.round(r.km() * 100) / 100.0,
                (int) Math.round(r.segundos() / 60.0), r.fuente());
    }
}
