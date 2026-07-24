package com.nakel.backend.repository;

import com.nakel.backend.model.CajaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaRepository extends JpaRepository<CajaDiaria, Long> {
    // Busca la caja abierta activa si existe
    Optional<CajaDiaria> findFirstByEstadoOrderByFechaAperturaDesc(String estado);

    // Trae todas las cajas ordenadas por fecha reciente (para el historial)
    List<CajaDiaria> findAllByOrderByFechaAperturaDesc();

    Optional<CajaDiaria> findFirstByOrderByFechaAperturaDesc();
}