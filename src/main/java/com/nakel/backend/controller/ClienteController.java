package com.nakel.backend.controller;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.service.ClienteService;
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
    public List<Cliente> obtenerTodos() {
        return service.obtenerTodos();
    }

    // 🔍 NUEVO: Endpoint para el buscador predictivo del Frontend
    // Se usa así: /api/clientes/buscar?nombre=Lio
    @GetMapping("/buscar")
    public List<Cliente> buscarPorNombre(@RequestParam String nombre) {
        return service.buscarPorNombre(nombre);
    }

    // 🛡️ MEJORADO: Atrapamos la explosión del CUIT duplicado
    @PostMapping
    public ResponseEntity<?> guardarCliente(@RequestBody Cliente cliente) {
        try {
            // Si todo sale bien, devolvemos el cliente con status 200 OK
            Cliente guardado = service.guardarCliente(cliente);
            return ResponseEntity.ok(guardado);
        } catch (RuntimeException e) {
            // Si el service frena la operación por CUIT duplicado,
            // le avisamos al Frontend con un 400 Bad Request y el mensaje exacto.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔍 NUEVO: Permite preguntar si un DNI ya existe antes de crearlo
    @GetMapping("/cuit/{cuit}")
    public ResponseEntity<Cliente> buscarPorCuit(@PathVariable String cuit) {
        return service.buscarPorCuit(cuit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // Devuelve 404 si no existe
    }
}