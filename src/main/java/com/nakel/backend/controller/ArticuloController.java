package com.nakel.backend.controller;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.service.ArticuloService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import com.nakel.backend.service.ReportePdfService; // nuevo service
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloController {

    private final ArticuloService articuloService;
    private final ReportePdfService reportePdfService;

    // Spring Boot inyecta el servicio automáticamente
    public ArticuloController(ArticuloService articuloService, ReportePdfService reportePdfService) {
        this.articuloService = articuloService;
        this.reportePdfService = reportePdfService; // 🔥 LO INICIALIZAMOS ACÁ
    }

    // 📋 GET: Obtener todos los artículos (Con soporte para Mostrador y Catálogo)
    @GetMapping
    public Page<Articulo> obtenerTodos(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) String origen,
            @RequestParam(required = false) Boolean stockDisponible, // 🔥 ATajamos lo que manda el Mostrador
            @PageableDefault(size = 50) Pageable pageable) {

        // 🛡️ REGLA DE ORO: Si es el Mostrador, filtramos por stock > 0
        if (Boolean.TRUE.equals(stockDisponible)) {
            return articuloService.buscarParaVenta(buscar, pageable);
        }

        // 📋 Si es el Catálogo, buscamos con todos los filtros normales
        return articuloService.buscarConFiltros(
                buscar,
                categoriaId,
                materialId,
                origen,
                pageable
        );
    }

    // 🔍 GET: Buscar por código de barras / SKU
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Articulo> buscarPorCodigo(@PathVariable String codigo) {
        Articulo articulo = articuloService.buscarPorCodigo(codigo);
        if (articulo != null) {
            return ResponseEntity.ok(articulo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // 💾 POST: Guardar nuevo artículo
    @PostMapping
    public ResponseEntity<?> guardarArticulo(@RequestBody Articulo articulo) {
        try {
            return ResponseEntity.ok(articuloService.guardarArticulo(articulo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se pudo guardar el artículo. Revisá los datos cargados.");
        }
    }

    // 🔄 PUT: Actualizar/Editar artículo existente
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarArticulo(@PathVariable Long id, @RequestBody Articulo articuloDetalles) {
        try {
            Articulo actualizado = articuloService.actualizarArticulo(id, articuloDetalles);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("No se pudo actualizar el artículo. Revisá los datos cargados.");
        }
    }

    // 🗑️ DELETE: Eliminar artículo por ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarArticulo(@PathVariable Long id) {
        try {
            boolean borrado = articuloService.eliminarArticulo(id);
            if (borrado) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(409).build();
        }
    }

    // 🔥 NUEVO: Sumar stock (cuando devuelven un producto)
    @PutMapping("/{id}/restaurar-stock")
    public ResponseEntity<?> restaurarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            Articulo actualizado = articuloService.restaurarStock(id, cantidad);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al restaurar stock: " + e.getMessage());
        }
    }

    // 🔥 NUEVO: Restar stock (cuando se llevan un producto en el cambio)
    @PutMapping("/{id}/descontar-stock")
    public ResponseEntity<?> descontarStock(@PathVariable Long id, @RequestParam int cantidad) {
        try {
            Articulo actualizado = articuloService.descontarStock(id, cantidad);
            if (actualizado != null) {
                return ResponseEntity.ok(actualizado);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al descontar stock: " + e.getMessage());
        }
    }
    @PutMapping("/aumentar-precios")
    public ResponseEntity<?> aumentarPrecios(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam BigDecimal porcentaje) {

        System.out.println("========== BACKEND AUMENTO MASIVO ==========");
        System.out.println("📦 Categoría ID: " + categoriaId);
        System.out.println("📈 Porcentaje: " + porcentaje);

        try {
            // Queda anotado en el historial de aumentos, para poder deshacerlo durante 7 días
            int cantidad = aumentoPreciosService.aumentarPorPorcentaje(categoriaId, porcentaje).getCantidadArticulos();

            System.out.println("✅ Artículos actualizados: " + cantidad);

            return ResponseEntity.ok(cantidad);

        } catch (Exception e) {
            System.err.println("❌ ERROR REAL EN AUMENTO MASIVO:");
            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body("Error al aumentar precios: " + e.getMessage());
        }
    }
    @org.springframework.beans.factory.annotation.Autowired
    private com.nakel.backend.service.AumentoPreciosService aumentoPreciosService;

    // 📜 Historial de cambios masivos de precios (de a 7, del más nuevo al más viejo)
    @GetMapping("/aumentos")
    public ResponseEntity<?> historialDeAumentos(@RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "7") int size) {
        return ResponseEntity.ok(aumentoPreciosService.historial(page, size));
    }

    // ↩️ Deshace un cambio masivo de precios (solo dentro de los 7 días)
    @PostMapping("/aumentos/{id}/deshacer")
    public ResponseEntity<?> deshacerAumento(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(aumentoPreciosService.deshacer(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.nakel.backend.service.EtiquetasPdfService etiquetasPdfService;

    // 🏷️ Etiquetas con código de barras, en hoja A4 de 24 etiquetas (70 x 37 mm).
    //    Ejemplos: /etiquetas?categoriaId=4&copias=STOCK      /etiquetas?idArticulo=12&copias=FIJA&cantidad=6
    @GetMapping("/etiquetas")
    public ResponseEntity<?> descargarEtiquetas(
            @RequestParam(required = false) Long idArticulo,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(defaultValue = "true") boolean soloConStock,
            @RequestParam(defaultValue = "UNA") String copias,
            @RequestParam(required = false) Integer cantidad,
            @RequestParam(defaultValue = "true") boolean conPrecio) {
        try {
            byte[] pdf = etiquetasPdfService.generar(idArticulo, categoriaId, soloConStock, copias, cantidad, conPrecio);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Etiquetas_Nakel.pdf");
            return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 🖨️ NUEVO: Descargar Reporte de Stock en PDF
    @GetMapping("/reporte-stock")
    public ResponseEntity<byte[]> descargarReporteStock(@RequestParam(required = false) Integer mayorA) {
        // mayorA: sin el parámetro = todos; 0 = solo con stock; N = con más de N unidades
        byte[] pdf = reportePdfService.generarReporteStock(mayorA);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Reporte_Stock_Nakel.pdf");

        return new ResponseEntity<>(pdf, headers, org.springframework.http.HttpStatus.OK);
    }
}
///