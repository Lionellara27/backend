package com.nakel.backend.repository;

import com.nakel.backend.model.Articulo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    // 🔥 SOLUCIÓN AL N+1:
    // Cruza categoría y material en una sola consulta.
    // Se conserva para los lugares donde realmente necesitemos
    // obtener todos los artículos de una vez.
    @Query("""
        SELECT a
        FROM Articulo a
        LEFT JOIN FETCH a.categoria
        LEFT JOIN FETCH a.material
        """)
    List<Articulo> findAllOptimizado();

    // 🔍 Buscar artículo por código/SKU
    Optional<Articulo> findByCodigo(String codigo);

    // 🔥 BÚSQUEDA + FILTROS + PAGINACIÓN REAL
    //
    // IMPORTANTE:
    // Esta consulta filtra EN LA BASE DE DATOS.
    // No trae los 20.000 artículos a Java para después filtrarlos.
    //
    // Ejemplo:
    // 20.000 artículos
    //       ↓
    // origen = PRODUCCION_PROPIA
    //       ↓
    // 10.000 resultados
    //       ↓
    // página de 50
    //       ↓
    // solamente trae 50 artículos al frontend.
    @Query("""
        SELECT a
        FROM Articulo a
        WHERE
            (
                :buscar IS NULL
                OR :buscar = ''
                OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
                OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :buscar, '%'))
            )
            AND (
                :categoriaId IS NULL
                OR a.categoria.id = :categoriaId
            )
            AND (
                :materialId IS NULL
                OR a.material.id = :materialId
            )
            AND (
                :origen IS NULL
                OR :origen = ''
                OR a.origen = :origen
            )
        """)
    Page<Articulo> buscarConFiltros(
            @Param("buscar") String buscar,
            @Param("categoriaId") Long categoriaId,
            @Param("materialId") Long materialId,
            @Param("origen") String origen,
            Pageable pageable
    );

    // 🔥 Cuenta el total de productos en el catálogo
    @Query("SELECT COUNT(a) FROM Articulo a")
    Integer contarProductosTotales();

    // 🔥 Cuenta cuántos productos están por debajo del stock mínimo
    @Query("SELECT COUNT(a) FROM Articulo a WHERE a.stockActual <= a.stockMinimo")
    Integer contarStockCritico();

    // 🔥 Busca los artículos de una categoría,
    // ordenados desde el último registrado.
    @Query("""
        SELECT a
        FROM Articulo a
        WHERE a.categoria.id = :categoriaId
        ORDER BY a.id DESC
        """)
    List<Articulo> findUltimoPorCategoria(
            @Param("categoriaId") Long categoriaId
    );

    @Query("""
    SELECT a
    FROM Articulo a
    LEFT JOIN a.categoria
    LEFT JOIN a.material
    WHERE a.stockActual > 0
      AND (
          :buscar IS NULL
          OR :buscar = ''
          OR LOWER(a.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
          OR LOWER(a.codigo) LIKE LOWER(CONCAT('%', :buscar, '%'))
          OR LOWER(a.categoria.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
          OR LOWER(a.material.nombre) LIKE LOWER(CONCAT('%', :buscar, '%'))
      )
    """)
    Page<Articulo> buscarParaVenta(
            @Param("buscar") String buscar,
            Pageable pageable
    );

    @Modifying
    @Transactional
    @Query("""
UPDATE Articulo a
SET a.precio = a.precio * (1 + :porcentaje / 100)
WHERE :categoriaId IS NULL
   OR a.categoria.id = :categoriaId
""")
    int aumentarPrecios(
            @Param("categoriaId") Long categoriaId,
            @Param("porcentaje") BigDecimal porcentaje
    );
}