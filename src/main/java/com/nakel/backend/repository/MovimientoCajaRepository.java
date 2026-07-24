package com.nakel.backend.repository;

import com.nakel.backend.model.MovimientoCaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoCajaRepository extends JpaRepository<MovimientoCaja, Long> {
    // 🔥 Le agregamos "Id" después de Caja para buscar solo por el número
    List<MovimientoCaja> findByCajaIdOrderByFechaHoraAsc(Long cajaId);
}