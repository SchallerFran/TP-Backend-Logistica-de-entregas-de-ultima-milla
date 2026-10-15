package utnfc.isi.back.logistica.pedidos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utnfc.isi.back.logistica.pedidos.dto.ClienteDTO;
import utnfc.isi.back.logistica.pedidos.dto.CrearClienteRequest;
import utnfc.isi.back.logistica.pedidos.model.Cliente;
import utnfc.isi.back.logistica.pedidos.repository.ClienteRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final String SUB = "sub-cliente-1";

    @Mock private ClienteRepository clienteRepository;
    @InjectMocks private ClienteService clienteService;

    private CrearClienteRequest request() {
        return new CrearClienteRequest("Distribuidora Sur SRL", "30-11111111-9", "ventas@sur.test", "351-5550000");
    }

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setKeycloakUserId(SUB);
        cliente.setRazonSocial("Distribuidora Sur SRL");
        cliente.setCuit("30-11111111-9");
        cliente.setEmail("ventas@sur.test");
        return cliente;
    }

    @Test
    @DisplayName("Perfil duplicado para el mismo sub: 409")
    void perfilDuplicado() {
        when(clienteRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.of(cliente()));

        assertThrows(ConflictoException.class, () -> clienteService.crearPerfil(SUB, request()));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("CUIT ya registrado por otro usuario: 409")
    void cuitDuplicado() {
        when(clienteRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(clienteRepository.existsByCuit("30-11111111-9")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> clienteService.crearPerfil(SUB, request()));

        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta correcta del perfil")
    void altaCorrecta() {
        when(clienteRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());
        when(clienteRepository.existsByCuit("30-11111111-9")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteDTO creado = clienteService.crearPerfil(SUB, request());

        assertTrue(creado.activo());
    }

    @Test
    @DisplayName("Desactivar usa la regla del modelo: el cliente deja de poder crear pedidos")
    void desactivar() {
        Cliente cliente = cliente();
        when(clienteRepository.findById(3L)).thenReturn(Optional.of(cliente));

        ClienteDTO resultado = clienteService.desactivar(3L);

        assertFalse(resultado.activo());
        assertFalse(cliente.puedeCrearPedidos());
    }

    @Test
    @DisplayName("Perfil inexistente: 404")
    void perfilInexistente() {
        when(clienteRepository.findByKeycloakUserId(SUB)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> clienteService.obtenerPerfil(SUB));
    }
}
