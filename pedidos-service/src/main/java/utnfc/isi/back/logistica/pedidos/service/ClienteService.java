package utnfc.isi.back.logistica.pedidos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utnfc.isi.back.logistica.pedidos.dto.ActualizarClienteRequest;
import utnfc.isi.back.logistica.pedidos.dto.ClienteDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearClienteRequest;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    /** Crea el perfil del usuario autenticado. El sub del JWT es la identidad. */
    @Transactional
    public ClienteDTO crearPerfil(String keycloakUserId, CrearClienteRequest request) {
        if (clienteRepository.findByKeycloakUserId(keycloakUserId).isPresent()) {
            throw new ConflictoException("El usuario ya tiene un perfil de cliente");
        }
        if (clienteRepository.existsByCuit(request.cuit())) {
            throw new ConflictoException("El CUIT " + request.cuit() + " ya esta registrado");
        }

        Cliente cliente = new Cliente();
        cliente.setKeycloakUserId(keycloakUserId);
        cliente.setRazonSocial(request.razonSocial());
        cliente.setCuit(request.cuit());
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        return ClienteDTO.from(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtenerPerfil(String keycloakUserId) {
        return ClienteDTO.from(buscarPorSub(keycloakUserId));
    }

    @Transactional
    public ClienteDTO actualizarPerfil(String keycloakUserId, ActualizarClienteRequest request) {
        Cliente cliente = buscarPorSub(keycloakUserId);
        cliente.setEmail(request.email());
        cliente.setTelefono(request.telefono());
        return ClienteDTO.from(cliente);
    }

    @Transactional(readOnly = true)
    public Page<ClienteDTO> listar(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(ClienteDTO::from);
    }

    /** Usa la regla del modelo: Cliente.desactivar(). */
    @Transactional
    public ClienteDTO desactivar(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.cliente(id));
        cliente.desactivar();
        return ClienteDTO.from(cliente);
    }

    private Cliente buscarPorSub(String keycloakUserId) {
        return clienteRepository.findByKeycloakUserId(keycloakUserId)
                .orElseThrow(RecursoNoEncontradoException::perfil);
    }
}
