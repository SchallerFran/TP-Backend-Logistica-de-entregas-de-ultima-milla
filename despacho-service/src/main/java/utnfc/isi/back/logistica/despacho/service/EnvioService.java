package utnfc.isi.back.logistica.despacho.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utnfc.isi.back.logistica.despacho.repository.EnvioRepository;

/**
 * Esqueleto de la capa de servicio. Sin logica de negocio todavia.
 */
@Service
@Transactional
public class EnvioService {

    private final EnvioRepository envioRepository;

    public EnvioService(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    @Transactional(readOnly = true)
    public long contar() {
        return envioRepository.count();
    }
}
