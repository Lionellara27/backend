package com.nakel.backend.service;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    public Cliente guardarCliente(Cliente cliente) {
        // En el futuro, si querés validar que el DNI no se repita, lo programás acá adentro.
        return clienteRepository.save(cliente);
    }
}