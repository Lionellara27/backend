package com.nakel.backend.repository;

import com.nakel.backend.model.Proveedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // 🔍 Busca por Razón Social y PAGINA los resultados
    Page<Proveedor> findByRazonSocialContainingIgnoreCase(
            String razonSocial,
            Pageable pageable
    );

    // 🔍 Busca por Nombre de Contacto y PAGINA los resultados
    Page<Proveedor> findByNombreContactoContainingIgnoreCase(
            String nombreContacto,
            Pageable pageable
    );

    // 🔍 Busca un CUIT exacto
    Optional<Proveedor> findByCuit(String cuit);
}