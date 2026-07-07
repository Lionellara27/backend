package com.nakel.backend.repository;

import com.nakel.backend.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // 🔍 NUEVO: Busca por Razón Social (ignora mayúsculas/minúsculas y busca partes del texto)
    List<Proveedor> findByRazonSocialContainingIgnoreCase(String razonSocial);

    // 🔍 NUEVO: Busca un CUIT exacto
    Optional<Proveedor> findByCuit(String cuit);
}