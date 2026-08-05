package com.nakel.backend.controller;

import com.nakel.backend.model.Proveedor;
import com.nakel.backend.service.ProveedorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    // 📋 TRAER TODOS
    @GetMapping
    public List<Proveedor> obtenerTodos() {
        // Usamos una lista simple para que el front lo procese rápido
        return service.obtenerTodos();
    }

    // ➕ GUARDAR NUEVO
    @PostMapping
    public ResponseEntity<?> guardarProveedor(@RequestBody Proveedor proveedor) {
        try {
            Proveedor guardado = service.guardar(proveedor);
            return ResponseEntity.ok(guardado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✏️ ACTUALIZAR (PUT)
    // ✏️ ACTUALIZAR (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProveedor(@PathVariable Long id, @RequestBody Proveedor proveedorActualizado) {
        try {
            // Buscamos si el proveedor existe, le pisamos los datos viejos con los nuevos, y guardamos
            return service.obtenerPorId(id).map(proveedorExistente -> {
                proveedorExistente.setRazonSocial(proveedorActualizado.getRazonSocial());
                proveedorExistente.setNombreContacto(proveedorActualizado.getNombreContacto());
                proveedorExistente.setTelefono(proveedorActualizado.getTelefono());
                proveedorExistente.setRubro(proveedorActualizado.getRubro());
                proveedorExistente.setCuit(proveedorActualizado.getCuit());
                proveedorExistente.setEmail(proveedorActualizado.getEmail());

                // 🔥 ACÁ ESTÁN LOS REEMPLAZOS
                proveedorExistente.setSaldoFavor(proveedorActualizado.getSaldoFavor());
                proveedorExistente.setSaldoContra(proveedorActualizado.getSaldoContra());
                proveedorExistente.setComentarios(proveedorActualizado.getComentarios());

                Proveedor guardado = service.guardar(proveedorExistente);
                return ResponseEntity.ok(guardado);
            }).orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🗑️ ELIMINAR (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProveedor(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/buscar")
    public List<Proveedor> buscarPorNombre(@RequestParam String nombre) {
        return service.buscarPorNombre(nombre);
    }

    // 🔍 Endpoint para verificar CUIT
    @GetMapping("/cuit/{cuit}")
    public ResponseEntity<Proveedor> buscarPorCuit(@PathVariable String cuit) {
        return service.buscarPorCuit(cuit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}