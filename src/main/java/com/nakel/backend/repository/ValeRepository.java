package com.nakel.backend.repository;

import com.nakel.backend.model.Vale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ValeRepository extends JpaRepository<Vale, Long> {

    // Buscar el vale leyendo el código alfanumérico
    Optional<Vale> findByCodigo(String codigo);
}