package com.nakel.backend.repository;

import com.nakel.backend.model.Insumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    // 🔥 BLINDAJE 1: @EntityGraph mata el N+1 (1 sola consulta SQL)
    // 🔥 BLINDAJE 2: Page<T> mata el consumo de RAM (trae de a ej: 20 registros)
    @Override
    @EntityGraph(attributePaths = {"categoria"})
    Page<Insumo> findAll(Pageable pageable);

    // 🔥 El buscador predictivo también blindado contra la RAM
    @EntityGraph(attributePaths = {"categoria"})
    Page<Insumo> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

}