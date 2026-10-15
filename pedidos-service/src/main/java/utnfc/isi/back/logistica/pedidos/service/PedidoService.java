package utnfc.isi.back.logistica.pedidos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utnfc.isi.back.logistica.pedidos.repository.PedidoRepository;

/**
 * Esqueleto de la capa de servicio. Sin logica de negocio todavia.
 */
@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public long contar() {
        return pedidoRepository.count();
    }
}
