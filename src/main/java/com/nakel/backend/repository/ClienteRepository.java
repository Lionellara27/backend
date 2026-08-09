package com.nakel.backend.repository;

import com.nakel.backend.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // 1. Búsqueda EXACTA por CUIT
    // Se mantiene para validar duplicados y buscar un cliente específico.
    Optional<Cliente> findByCuit(String cuit);

    // 2. Búsqueda por NOMBRE + PAGINACIÓN
    Page<Cliente> findByNombreContainingIgnoreCase(
            String nombre,
            Pageable pageable
    );

    // 3. Búsqueda PARCIAL por CUIT + PAGINACIÓN
    // Permite encontrar el CUIT aunque el usuario escriba solo una parte.
    Page<Cliente> findByCuitContainingIgnoreCase(
            String cuit,
            Pageable pageable
    );

    // 4. BLINDAJE ANTI N+1 (Fetch Join)
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.ventas")
    Page<Cliente> findAllConVentas(Pageable pageable);

    // El findAll(Pageable) ya viene heredado de JpaRepository.
}