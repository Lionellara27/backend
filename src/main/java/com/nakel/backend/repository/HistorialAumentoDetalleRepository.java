package com.nakel.backend.repository;

import com.nakel.backend.model.HistorialAumentoDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialAumentoDetalleRepository extends JpaRepository<HistorialAumentoDetalle, Long> {

    List<HistorialAumentoDetalle> findByAumentoId(Long aumentoId);
}
