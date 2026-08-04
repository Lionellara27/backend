package com.nakel.backend.repository;

import com.nakel.backend.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // 🔥 Suma total de plata en un rango de fechas (EXCLUYENDO PRESUPUESTOS)
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND v.tipoComprobante != 'Presupuesto'")
    java.math.BigDecimal sumarVentasEntreFechas(@org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio, @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);

    // 🔥 Cuenta cuántos tickets se hicieron en un rango de fechas (EXCLUYENDO PRESUPUESTOS)
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM Venta v WHERE v.fechaHora >= :inicio AND v.fechaHora <= :fin AND v.tipoComprobante != 'Presupuesto'")
    Integer contarVentasEntreFechas(@org.springframework.data.repository.query.Param("inicio") java.time.LocalDateTime inicio, @org.springframework.data.repository.query.Param("fin") java.time.LocalDateTime fin);
}