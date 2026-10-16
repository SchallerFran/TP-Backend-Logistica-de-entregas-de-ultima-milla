package utnfc.isi.back.logistica.despacho.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj inyectable en la zona horaria de la operación. Evita depender de la zona del contenedor
 * (UTC por defecto) y permite fijar la hora en tests.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(DespachoProperties props) {
        return Clock.system(ZoneId.of(props.zonaHoraria()));
    }
}
