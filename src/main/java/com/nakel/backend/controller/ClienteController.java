package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.service.ClienteService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Cliente> obtenerTodos() {
        return service.obtenerTodos();
    }

    @PostMapping
    public Cliente guardarCliente(@RequestBody Cliente cliente) {
        return service.guardarCliente(cliente);
    }
}