package com.nakel.backend.repository;

import com.nakel.backend.model.CategoriaInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaInsumoRepository extends JpaRepository<CategoriaInsumo, Long> {

    // Spring ya te regala el save(), findAll(), findById(), deleteById(), etc.
    // Opcional: si en el futuro querés buscar una categoría exacta por nombre:
    // Optional<CategoriaInsumo> findByNombre(String nombre);
}