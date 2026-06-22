package com.nakel.backend.repository;

import com.nakel.backend.model.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticuloRepository extends JpaRepository<Articulo, Long> {

    // 🔥 SOLUCIÓN AL N+1: Le decimos que cruce las tablas en un solo viaje
    @Query("SELECT a FROM Articulo a LEFT JOIN FETCH a.categoria LEFT JOIN FETCH a.material")
    List<Articulo> findAllOptimizado();

    Optional<Articulo> findByCodigo(String codigo);
}