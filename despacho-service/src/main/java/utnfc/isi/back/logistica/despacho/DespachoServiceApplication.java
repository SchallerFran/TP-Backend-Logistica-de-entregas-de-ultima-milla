package utnfc.isi.back.logistica.despacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "utnfc.isi.back.logistica.despacho",
        "utnfc.isi.back.logistica.commons"
})
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class DespachoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DespachoServiceApplication.class, args);
    }
}
