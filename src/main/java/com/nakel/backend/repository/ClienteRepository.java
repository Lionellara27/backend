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

    // 1. Busqueda exacta para validación
    Optional<Cliente> findByCuit(String cuit);

    // 2. BUSCADOR PREDICTIVO PAGINADO
    // Usamos Pageable para no traer 10.000 clientes juntos a la RAM
    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // 3. BLINDAJE ANTI N+1 (Fetch Join)
    // Si mañana le agregás una relación a Cliente (como 'ventas'), usá este patrón:
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.ventas")
    Page<Cliente> findAllConVentas(Pageable pageable);

    // Para el listar general paginado, simplemente usás el que ya heredas de JpaRepository:
    // Page<Cliente> findAll(Pageable pageable);
}