package com.nakel.backend.service;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.repository.ClienteRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    // 1. 🔥 Listado Paginado: Adiós a traer miles de registros de golpe
    @Transactional(readOnly = true)
    public Page<Cliente> obtenerTodos(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscarGlobal(String texto, Pageable pageable) {

        if (texto == null || texto.isBlank()) {
            return repository.findAll(pageable);
        }

        String busqueda = texto.trim();

        // Si parece un CUIT/DNI, buscamos por CUIT
        if (busqueda.matches("\\d+")) {
            return repository.findByCuitContainingIgnoreCase(
                    busqueda,
                    pageable
            );
        }

        // Si no, buscamos por nombre
        return repository.findByNombreContainingIgnoreCase(
                busqueda,
                pageable
        );
    }

    // 3. Validación de CUIT (Se mantiene igual, ¡está muy bien!)
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        if (cliente.getCuit() != null && !cliente.getCuit().isBlank()) {
            Optional<Cliente> existente = repository.findByCuit(cliente.getCuit());
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

    // 4. Actualización (Se mantiene igual, bien lograda)
    // 4. Actualización
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        Cliente existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: No se encontró el cliente."));

        existente.setNombre(clienteActualizado.getNombre());
        existente.setCuit(clienteActualizado.getCuit());
        existente.setCondicionIva(clienteActualizado.getCondicionIva());
        existente.setTelefono(clienteActualizado.getTelefono());
        existente.setEmail(clienteActualizado.getEmail());

        // 🔥 AGREGAMOS ESTO: Mapeamos los nuevos saldos para permitir edición manual
        existente.setSaldoAFavor(clienteActualizado.getSaldoAFavor());
        existente.setSaldoPendiente(clienteActualizado.getSaldoPendiente());

        return guardarCliente(existente);
    }

    // 5. Eliminación (Se mantiene igual, bien lograda)
    @Transactional
    public void eliminarCliente(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Error: El cliente ya no existe.");
        }
        try {
            repository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("No se puede eliminar: tiene ventas asociadas.");
        }
    }
}