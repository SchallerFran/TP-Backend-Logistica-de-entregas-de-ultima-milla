package utnfc.isi.back.logistica.despacho.config;

import utnfc.isi.back.logistica.despacho.service.planificacion.CalculadoraRuta;
import utnfc.isi.back.logistica.despacho.service.planificacion.PlanificadorRutas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** El núcleo de planificación es Java puro; acá se lo registra como beans de Spring. */
@Configuration
public class PlanificacionConfig {

    @Bean
    CalculadoraRuta calculadoraRuta() {
        return new CalculadoraRuta();
    }

    @Bean
    PlanificadorRutas planificadorRutas(CalculadoraRuta calculadora) {
        return new PlanificadorRutas(calculadora);
    }
}
