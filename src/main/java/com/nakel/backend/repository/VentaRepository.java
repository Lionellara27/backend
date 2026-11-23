package com.nakel.backend.repository;

import com.nakel.backend.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Cuántas ventas tiene un cliente (para impedir borrar clientes con historial)
    long countByClienteId(Long clienteId);

    // Historial con filtros. El mes se filtra por RANGO de fechas (desde/hasta):
    // SQLite guarda la fecha como número, así que buscar el texto "-09-" no encontraba nada.
    @EntityGraph(attributePaths = {"cliente"})
    @org.springframework.data.jpa.repository.Query(
            value = """
            SELECT v FROM Venta v LEFT JOIN v.cliente c
            WHERE (:sinFecha = true OR (v.fechaHora >= :desde AND v.fechaHora < :hasta))
            AND (:buscar IS NULL OR :buscar = ''
                 OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
                 OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
                )
        """,
            countQuery = """
            SELECT COUNT(v) FROM Venta v LEFT JOIN v.cliente c
            WHERE (:sinFecha = true OR (v.fechaHora >= :desde AND v.fechaHora < :hasta))
            AND (:buscar IS NULL OR :buscar = ''
                 OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
                 OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
                )
        """
    )
    Page<Venta> buscarConFiltros(@Param("buscar") String buscar, @Param("criterio") String criterio,
                                 @Param("sinFecha") boolean sinFecha,
                                 @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                                 Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
        SELECT SUM(v.total) FROM Venta v LEFT JOIN v.cliente c
        WHERE (v.tipoComprobante IS NULL OR v.tipoComprobante <> 'Presupuesto')
        AND (:sinFecha = true OR (v.fechaHora >= :desde AND v.fechaHora < :hasta))
        AND (:buscar IS NULL OR :buscar = ''
             OR (:criterio = 'Nro. Comprobante' AND CAST(v.id AS string) = :buscar)
             OR (:criterio = 'Cliente (Nombre/DNI)' AND (LOWER(c.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR LOWER(c.cuit) LIKE LOWER(CONCAT('%', :buscar, '%'))))
            )
    """)
    Double obtenerTotalGlobal(@Param("buscar") String buscar, @Param("criterio") String criterio,
                              @Param("sinFecha") boolean sinFecha,
                              @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    // -----------------------------------------------------------------------------
    // CONSULTAS PARA ESTADÍSTICAS
    // -----------------------------------------------------------------------------

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND (v.tipoComprobante IS NULL OR v.tipoComprobante <> 'Presupuesto')")
    java.math.BigDecimal sumarVentasEntreFechas(@Param("inicio") java.time.LocalDateTime inicio, @Param("fin") java.time.LocalDateTime fin);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND (v.tipoComprobante IS NULL OR v.tipoComprobante <> 'Presupuesto')")
    Integer contarVentasEntreFechas(@Param("inicio") java.time.LocalDateTime inicio, @Param("fin") java.time.LocalDateTime fin);
}
