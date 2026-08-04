package com.nakel.backend.repository;

import com.nakel.backend.model.Cambio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CambioRepository extends JpaRepository<Cambio, Long> {

    // 🔥 Trae todo el historial de cambios de un ticket en particular
    List<Cambio> findByVentaOriginalId(Long ventaId);
}