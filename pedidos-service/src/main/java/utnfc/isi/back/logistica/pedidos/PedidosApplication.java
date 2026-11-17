package utnfc.isi.back.logistica.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "utnfc.isi.back.logistica.pedidos",
        "utnfc.isi.back.logistica.commons"
})
@EnableAsync
public class PedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosApplication.class, args);
    }
}
