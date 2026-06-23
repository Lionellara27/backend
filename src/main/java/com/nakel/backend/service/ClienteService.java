package com.nakel.backend.service;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    // 1. Inyección por constructor (Adiós @Autowired)
    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // 2. 🔥 Apagamos el rastreo de memoria RAM
    @Transactional(readOnly = true)
    public List<Cliente> obtenerTodos() {
        return repository.findAll();
    }

    // 3. Exponemos el buscador predictivo para cuando lo necesite el Frontend
    @Transactional(readOnly = true)
    public List<Cliente> buscarPorNombre(String nombre) {
        return repository.findByNombreContainingIgnoreCase(nombre);
    }

    // 4. 🔥 Hacemos realidad tu comentario: Validación de CUIT duplicado
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        // Solo validamos si el cajero efectivamente ingresó un CUIT
        if (cliente.getCuit() != null && !cliente.getCuit().isBlank()) {
            Optional<Cliente> existente = repository.findByCuit(cliente.getCuit());

            // Si existe y NO es el mismo cliente que estamos editando, explotamos
            if (existente.isPresent() && !existente.get().getId().equals(cliente.getId())) {
                throw new RuntimeException("Error: Ya existe un cliente registrado con el CUIT/DNI " + cliente.getCuit());
            }
        }
        return repository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorCuit(String cuit) {
        return repository.findByCuit(cuit);
    }
}