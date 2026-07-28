package com.nakel.backend.service;

import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public Articulo guardarArticulo(Articulo articulo) {
        return articuloRepository.save(articulo);
    }

    public Articulo buscarPorCodigo(String codigo) {
        return articuloRepository.findByCodigo(codigo).orElse(null);
    }

    // En tu ArticuloService.java del Backend:

    public boolean eliminarArticulo(Long id) {
        if (articuloRepository.existsById(id)) {
            articuloRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Articulo actualizarArticulo(Long id, Articulo articuloNuevo) {
        return articuloRepository.findById(id).map(art -> {
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
}