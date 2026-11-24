package com.nakel.backend.repository;

import com.nakel.backend.model.Vale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ValeRepository extends JpaRepository<Vale, Long> {

    // Buscar el vale leyendo el código alfanumérico
    Optional<Vale> findByCodigo(String codigo);

    // SALDO A FAVOR de un cliente = suma de sus vales activos que todavía no vencieron.
    // No es un número que se escribe: sale siempre de los vales.
    @Query("""
        SELECT COALESCE(SUM(v.monto), 0)
        FROM Vale v
        WHERE v.idCliente = :idCliente AND v.estado = 'ACTIVO' AND v.fechaVencimiento > :ahora
        """)
    BigDecimal sumarVigentesDeCliente(@Param("idCliente") Long idCliente, @Param("ahora") LocalDateTime ahora);

    // Lo mismo para varios clientes a la vez (una página de la agenda): devuelve pares [idCliente, suma]
    @Query("""
        SELECT v.idCliente, SUM(v.monto)
        FROM Vale v
        WHERE v.idCliente IN :ids AND v.estado = 'ACTIVO' AND v.fechaVencimiento > :ahora
        GROUP BY v.idCliente
        """)
    java.util.List<Object[]> sumarVigentesDeClientes(@Param("ids") java.util.Collection<Long> ids, @Param("ahora") LocalDateTime ahora);

    // Los vales vigentes de un cliente, uno por uno (para mostrar de dónde sale su saldo a favor)
    java.util.List<Vale> findByIdClienteAndEstadoAndFechaVencimientoAfterOrderByFechaEmisionAsc(
            Long idCliente, String estado, LocalDateTime ahora);

    // Listados para la pantalla de Vales (más nuevos primero)
    Page<Vale> findAllByOrderByFechaEmisionDesc(Pageable pageable);

    Page<Vale> findByEstadoAndFechaVencimientoAfterOrderByFechaEmisionDesc(String estado, LocalDateTime ahora, Pageable pageable);
}
