package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    public Page<Cliente> obtenerTodos(Pageable pageable) {
        return service.obtenerTodos(pageable);
    }

    // 🔍 NUEVO: Endpoint para el buscador predictivo del Frontend
    @GetMapping("/buscar")
    public Page<Cliente> buscarPorNombre(@RequestParam String nombre, Pageable pageable) {
        return service.buscarPorNombre(nombre, pageable);
    }

    // 🛡️ MEJORADO: Atrapamos la explosión del CUIT duplicado
    @PostMapping
    public ResponseEntity<?> guardarCliente(@RequestBody Cliente cliente) {
        try {
            Cliente guardado = service.guardarCliente(cliente);
            return ResponseEntity.ok(guardado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔍 NUEVO: Permite preguntar si un DNI ya existe antes de crearlo
    @GetMapping("/cuit/{cuit}")
    public ResponseEntity<Cliente> buscarPorCuit(@PathVariable String cuit) {
        return service.buscarPorCuit(cuit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ✏️ NUEVO: Endpoint para Actualizar (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(@PathVariable Long id, @RequestBody Cliente clienteActualizado) {
        try {
            Cliente actualizado = service.actualizarCliente(id, clienteActualizado);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🗑️ NUEVO: Endpoint para Borrar (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        try {
            service.eliminarCliente(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}