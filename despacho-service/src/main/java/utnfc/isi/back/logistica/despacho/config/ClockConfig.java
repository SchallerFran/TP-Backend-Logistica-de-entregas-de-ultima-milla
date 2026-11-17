package utnfc.isi.back.logistica.despacho.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    @Bean
    Clock clock(DespachoProperties props) {
        return Clock.system(ZoneId.of(props.zonaHoraria()));
    }
}
