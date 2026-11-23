package com.nakel.backend.repository;

import com.nakel.backend.model.HistorialAumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialAumentoRepository extends JpaRepository<HistorialAumento, Long> {

    // Del más nuevo al más viejo
    Page<HistorialAumento> findAllByOrderByFechaDescIdDesc(Pageable pageable);
}
