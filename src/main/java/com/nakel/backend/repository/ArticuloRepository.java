package com.nakel.backend.repository;

import com.nakel.backend.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    // 🔥 SOLUCIÓN AL N+1: Le decimos que cruce las tablas en un solo viaje
    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria LEFT JOIN FETCH a.material")
    List<Articulo> findAllOptimizado();

    Optional<Articulo> findByCodigo(String codigo);

    // 🔥 Cuenta el total de productos en el catálogo
    @Query("SELECT COUNT(a) FROM Articulo a")
    Integer contarProductosTotales();

    // 🔥 Cuenta cuántos productos están por debajo del stock mínimo (ALERTA ROJA)
    @Query("SELECT COUNT(a) FROM Articulo a WHERE a.stockActual <= a.stockMinimo")
    Integer contarStockCritico();

    // 🔥 Busca el último artículo registrado de una categoría específica para ordenar el SKU
    // 🔥 Busca el último artículo registrado de una categoría específica para ordenar el SKU
    @Query("SELECT a FROM Articulo a WHERE a.categoria.id = :categoriaId ORDER BY a.id DESC")
    List<Articulo> findUltimoPorCategoria(@org.springframework.data.repository.query.Param("categoriaId") Long categoriaId);

}