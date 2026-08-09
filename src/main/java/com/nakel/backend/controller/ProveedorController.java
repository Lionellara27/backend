package com.nakel.backend.controller;

import com.nakel.backend.model.Proveedor;
import com.nakel.backend.service.ProveedorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins = "*")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    // 📋 TRAER TODOS PAGINADOS / BUSCAR GLOBALMENTE
    @GetMapping
    public ResponseEntity<Page<Proveedor>> obtenerTodos(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) String campo,
            Pageable pageable) {

        if (buscar != null && !buscar.isBlank()) {
            String texto = buscar.trim();

            if ("Contacto".equalsIgnoreCase(campo)) {
                return ResponseEntity.ok(
                        service.buscarPorContacto(texto, pageable)
                );
            }

            // Por defecto: Empresa
            return ResponseEntity.ok(
                    service.buscarPorNombre(texto, pageable)
            );
        }

        return ResponseEntity.ok(service.obtenerTodos(pageable));
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

    // ✏️ ACTUALIZAR
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProveedor(@PathVariable Long id, @RequestBody Proveedor proveedorActualizado) {
        try {
            return service.obtenerPorId(id).map(proveedorExistente -> {
                proveedorExistente.setRazonSocial(proveedorActualizado.getRazonSocial());
                proveedorExistente.setNombreContacto(proveedorActualizado.getNombreContacto());
                proveedorExistente.setTelefono(proveedorActualizado.getTelefono());
                proveedorExistente.setRubro(proveedorActualizado.getRubro());
                proveedorExistente.setCuit(proveedorActualizado.getCuit());
                proveedorExistente.setEmail(proveedorActualizado.getEmail());

                // 🔥 REEMPLAZOS
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

    // 🗑️ ELIMINAR
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProveedor(@PathVariable Long id) {
        try {
            service.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🔍 Endpoint para verificar CUIT
    @GetMapping("/cuit/{cuit}")
    public ResponseEntity<Proveedor> buscarPorCuit(@PathVariable String cuit) {
        return service.buscarPorCuit(cuit)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
