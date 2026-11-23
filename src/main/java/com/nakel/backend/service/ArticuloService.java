package com.nakel.backend.service;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
public class ArticuloService {

    private final ArticuloRepository articuloRepository;

    public ArticuloService(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    // 🔥 Apagamos el rastreo de memoria RAM y usamos la consulta de 1 solo viaje
    @Transactional(readOnly = true)
    public List<Articulo> obtenerTodos() {
        return articuloRepository.findAllOptimizado();
    }

    // 🔥 BÚSQUEDA + FILTROS + PAGINACIÓN REAL
    @Transactional(readOnly = true)
    public Page<Articulo> buscarConFiltros(
            String buscar,
            Long categoriaId,
            Long materialId,
            String origen,
            Pageable pageable) {

        return articuloRepository.buscarConFiltros(
                buscar,
                categoriaId,
                materialId,
                origen,
                pageable
        );
    }

    // 🛒 BÚSQUEDA ESPECÍFICA PARA VENTA
    // Busca en TODO el catálogo desde la BD,
    // pero devuelve únicamente artículos con stock disponible.
    @Transactional(readOnly = true)
    public Page<Articulo> buscarParaVenta(
            String buscar,
            Pageable pageable) {

        return articuloRepository.buscarParaVenta(
                buscar,
                pageable
        );
    }

    @Transactional
    public Articulo guardarArticulo(Articulo articulo) {
        validarDatos(articulo);
        liberarCodigo(articulo.getCodigo(), articulo.getId());
        if (articulo.getAlicuotaIva() == null) {
            articulo.setAlicuotaIva(21.0);
        }
        if (articulo.getStockMinimo() == null) {
            articulo.setStockMinimo(8);
        }
        return articuloRepository.save(articulo);
    }

    @Transactional(readOnly = true)
    public Articulo buscarPorCodigo(String codigo) {
        return articuloRepository.findByCodigo(codigo).orElse(null);
    }

    // 🔥 ACÁ ESTÁ LA MAGIA: Borrado Lógico en vez de Borrado Físico
    @Transactional
    public boolean eliminarArticulo(Long id) {
        return articuloRepository.findById(id).map(art -> {
            art.setActivo(false); // Ocultamos el artículo
            articuloRepository.save(art); // Guardamos el nuevo estado
            return true;
        }).orElse(false);
    }

    @Transactional
    public Articulo actualizarArticulo(Long id, Articulo articuloNuevo) {
        validarDatos(articuloNuevo);
        return articuloRepository.findById(id).map(art -> {
            liberarCodigo(articuloNuevo.getCodigo(), id);
            art.setCodigo(articuloNuevo.getCodigo());
            art.setNombre(articuloNuevo.getNombre());
            art.setPrecio(articuloNuevo.getPrecio());
            art.setStockActual(articuloNuevo.getStockActual());
            art.setOrigen(articuloNuevo.getOrigen());
            art.setCategoria(articuloNuevo.getCategoria());
            art.setMaterial(articuloNuevo.getMaterial());
            return articuloRepository.save(art);
        }).orElse(null);
    }

    // Validaciones básicas con mensajes que la usuaria pueda entender
    private void validarDatos(Articulo articulo) {
        if (articulo.getCodigo() == null || articulo.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código del artículo es obligatorio.");
        }
        articulo.setCodigo(articulo.getCodigo().trim());
        if (articulo.getNombre() == null || articulo.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del artículo es obligatorio.");
        }
        if (articulo.getPrecio() == null || articulo.getPrecio().signum() < 0) {
            throw new IllegalArgumentException("El precio debe ser un número mayor o igual a 0.");
        }
        if (articulo.getStockActual() == null || articulo.getStockActual() < 0) {
            throw new IllegalArgumentException("El stock debe ser un número mayor o igual a 0.");
        }
    }

    /**
     * El código es ÚNICO en la base aunque el artículo esté dado de baja (borrado lógico).
     * - Si lo usa otro artículo ACTIVO: error claro.
     * - Si lo usa uno dado de baja: le cambiamos el código al viejo para liberar el número
     *   (el artículo viejo sigue existiendo para no romper el historial de ventas).
     */
    @Transactional
    public void liberarCodigo(String codigo, Long idPropio) {
        if (codigo == null) return;
        Optional<Articulo> existente = articuloRepository.findFirstByCodigo(codigo.trim());
        if (existente.isEmpty()) return;

        Articulo otro = existente.get();
        if (otro.getId().equals(idPropio)) return;

        if (otro.isActivo()) {
            throw new IllegalArgumentException("Ya existe un artículo con el código " + codigo
                    + " (" + otro.getNombre() + ").");
        }

        otro.setCodigo(otro.getCodigo() + "-BAJA-" + otro.getId());
        articuloRepository.saveAndFlush(otro);
    }

    // 🔥 NUEVO: Suma stock al artículo cuando el cliente lo devuelve
    @Transactional
    public Articulo restaurarStock(Long id, int cantidad) {
        return articuloRepository.findById(id).map(art -> {
            art.setStockActual(art.getStockActual() + cantidad);
            return articuloRepository.save(art);
        }).orElse(null);
    }

    // 🔥 Resta stock al artículo cuando el cliente se lo lleva en un cambio
    @Transactional
    public Articulo descontarStock(Long id, int cantidad) {
        return articuloRepository.findById(id).map(art -> {
            if (art.getStockActual() < cantidad) {
                throw new IllegalArgumentException("Stock insuficiente de " + art.getNombre()
                        + " (quedan " + art.getStockActual() + ").");
            }
            art.setStockActual(art.getStockActual() - cantidad);
            return articuloRepository.save(art);
        }).orElse(null);
    }

    // El aumento masivo de precios se mudó a AumentoPreciosService (guarda historial y se puede deshacer).
}
