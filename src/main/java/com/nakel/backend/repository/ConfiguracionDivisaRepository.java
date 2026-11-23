package com.nakel.backend.repository;

import com.nakel.backend.model.ConfiguracionDivisa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionDivisaRepository extends JpaRepository<ConfiguracionDivisa, Long> {
}
