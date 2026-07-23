package com.nakel.backend.service;

import com.nakel.backend.model.Usuario;
import com.nakel.backend.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 🔥 Semilla inicial: Si la base de datos está vacía, crea por defecto el admin / 1234
    @PostConstruct
    public void inicializarAdminPorDefecto() {
        if (usuarioRepository.count() == 0) {
            Usuario adminDefault = new Usuario("admin", "1234");
            usuarioRepository.save(adminDefault);
            System.out.println("🛡️ Usuario por defecto creado: admin / 1234");
        }
    }

    // Validar Credenciales para el Login
    public boolean validarLogin(String username, String password) {
        return usuarioRepository.findByNombreUsuario(username)
                .map(usuario -> usuario.getContraseña().equals(password))
                .orElse(false);
    }

    // Actualizar Credenciales desde Configuración
    public void actualizarCredenciales(String usernameActual, String nuevoUsuario, String nuevaContraseña) throws Exception {
        // Buscamos al usuario (asumimos que operamos sobre el primer usuario o el admin actual)
        Usuario usuario = usuarioRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new Exception("No se encontró ningún usuario configurado."));

        if (nuevoUsuario != null && !nuevoUsuario.isBlank()) {
            usuario.setNombreUsuario(nuevoUsuario.trim());
        }
        if (nuevaContraseña != null && !nuevaContraseña.isBlank()) {
            usuario.setContraseña(nuevaContraseña.trim());
        }

        usuarioRepository.save(usuario);
    }
}