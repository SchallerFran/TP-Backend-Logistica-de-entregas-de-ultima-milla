package utnfc.isi.back.logistica.despacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * scanBasePackages incluye el paquete del JAR comun para levantar
 * GlobalExceptionHandler y KeycloakRoleConverter sin auto-configuracion.
 */
@SpringBootApplication(scanBasePackages = {
        "utnfc.isi.back.logistica.despacho",
        "utnfc.isi.back.logistica.commons"
})
public class DespachoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DespachoApplication.class, args);
    }
}
