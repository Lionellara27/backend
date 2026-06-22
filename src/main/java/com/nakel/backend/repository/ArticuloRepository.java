package com.nakel.backend.repository;

import com.nakel.backend.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {
    Optional<Articulo> findByCodigo(String codigo);
}