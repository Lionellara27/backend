package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*") // Permite que cualquier frontend se conecte sin problemas
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // Método para OBTENER todos los clientes
    @GetMapping
    public List<Cliente> obtenerTodos() {
        return clienteService.obtenerTodos();
    }

    // Método para CREAR un cliente nuevo
    @PostMapping
    public Cliente guardarCliente(@RequestBody Cliente cliente) {
        return clienteService.guardarCliente(cliente);
    }
}