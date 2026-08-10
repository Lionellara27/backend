package com.nakel.backend.repository;

import com.nakel.backend.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // 🔥 ARREGLO DEFINITIVO: Sin JOIN FETCH, con Count explícito y EntityGraph (Cero N+1 y paginación perfecta)
    @EntityGraph(attributePaths = {"cliente"})
    @org.springframework.data.jpa.repository.Query(
            value = """
            SELECT v FROM Venta v LEFT JOIN v.cliente c 
            WHERE (:mesStr = '00' OR CAST(v.fechaHora AS string) LIKE CONCAT('%-', :mesStr, '-%')) 
            AND (:buscar IS NULL OR :buscar = '' 
                 OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
                 OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
                )
        """,
            countQuery = """
            SELECT COUNT(v) FROM Venta v LEFT JOIN v.cliente c 
            WHERE (:mesStr = '00' OR CAST(v.fechaHora AS string) LIKE CONCAT('%-', :mesStr, '-%')) 
            AND (:buscar IS NULL OR :buscar = '' 
                 OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
                 OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
                )
        """
    )
    Page<Venta> buscarConFiltros(@Param("buscar") String buscar, @Param("criterio") String criterio, @Param("mesStr") String mesStr, Pageable pageable);

    // 🔥 La del total queda INTACTA porque usa LEFT JOIN simple (no rompe nada)
    @org.springframework.data.jpa.repository.Query("""
        SELECT SUM(v.total) FROM Venta v LEFT JOIN v.cliente c 
        WHERE v.tipoComprobante != 'Presupuesto' 
        AND (:mesStr = '00' OR CAST(v.fechaHora AS string) LIKE CONCAT('%-', :mesStr, '-%')) 
        AND (:buscar IS NULL OR :buscar = '' 
             OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
             OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
            )
    """)
    Double obtenerTotalGlobal(@Param("buscar") String buscar, @Param("criterio") String criterio, @Param("mesStr") String mesStr);

    // -----------------------------------------------------------------------------
    // TUS CONSULTAS ORIGINALES (INTACTAS PARA FUTURAS ESTADÍSTICAS)
    // -----------------------------------------------------------------------------

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND v.tipoComprobante != 'Presupuesto'")
    java.math.BigDecimal sumarVentasEntreFechas(@Param("inicio") java.time.LocalDateTime inicio, @Param("fin") java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND v.tipoComprobante != 'Presupuesto'")
    Integer contarVentasEntreFechas(@Param("inicio") java.time.LocalDateTime inicio, @Param("fin") java.time.LocalDateTime fin);
}