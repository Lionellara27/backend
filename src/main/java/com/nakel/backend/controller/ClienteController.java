package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.service.ClienteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    // 📋 TRAER TODOS PAGINADOS / 🔍 BUSCAR GLOBALMENTE
    @GetMapping
    public Page<Cliente> obtenerTodos(
            @RequestParam(required = false) String buscar,
            Pageable pageable) {

        if (buscar != null && !buscar.isBlank()) {
            return service.buscarGlobal(buscar.trim(), pageable);
        }

        return service.obtenerTodos(pageable);
    }

    // 🛡️ Verificar si existe CUIT
    @GetMapping("/cuit/{cuit}")
    public ResponseEntity<Cliente> buscarPorCuit(
            @PathVariable String cuit) {

        return service.buscarPorCuit(cuit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ➕ GUARDAR
    @PostMapping
    public ResponseEntity<?> guardarCliente(
            @RequestBody Cliente cliente) {

        try {
            Cliente guardado = service.guardarCliente(cliente);
            return ResponseEntity.ok(guardado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✏️ ACTUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCliente(
            @PathVariable Long id,
            @RequestBody Cliente clienteActualizado) {

        try {
            Cliente actualizado =
                    service.actualizarCliente(id, clienteActualizado);

            return ResponseEntity.ok(actualizado);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🗑️ ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(
            @PathVariable Long id) {

        try {
            service.eliminarCliente(id);
            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}