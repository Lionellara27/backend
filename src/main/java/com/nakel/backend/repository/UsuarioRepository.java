package com.nakel.backend.repository;

import com.nakel.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método mágico de Spring Data para buscar por nombre de usuario
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
}