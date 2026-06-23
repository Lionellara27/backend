package com.nakel.backend.repository;

import com.nakel.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // 1. Para evitar duplicados y para la búsqueda exacta con el lector/teclado
    Optional<Cliente> findByCuit(String cuit);

    // 2. Para el buscador "predictivo" (Ej: tipeás "Mar" y trae a "María" y "Marcelo")
    // Containing = LIKE %nombre% | IgnoreCase = No importa si escriben mayúsculas o minúsculas
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
}