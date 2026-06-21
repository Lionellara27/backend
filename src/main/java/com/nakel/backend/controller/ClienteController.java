package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*") // Permite que cualquier frontend se conecte sin problemas
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    // Método para OBTENER todos los clientes (Lo va a usar el buscador del mostrador)
    @GetMapping
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    // Método para CREAR un cliente nuevo
    @PostMapping
    public Cliente guardarCliente(@RequestBody Cliente cliente) {
        return clienteRepository.save(cliente);
    }
}