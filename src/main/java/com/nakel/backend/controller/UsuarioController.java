package com.nakel.backend.controller;

import com.nakel.backend.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Endpoint para el Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        String username = credenciales.get("username");
        String password = credenciales.get("password");

        boolean valido = usuarioService.validarLogin(username, password);
        if (valido) {
            return ResponseEntity.ok(Map.of("mensaje", "Login exitoso"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario o contraseña incorrectos");
        }
    }

    // Endpoint para actualizar credenciales desde Configuración
    @PutMapping("/actualizar")
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody Map<String, String> datos) {
        try {
            String usernameActual = datos.get("usernameActual");
            String nuevoUsuario = datos.get("nuevoUsuario");
            String nuevaContrasena = datos.get("nuevaContrasena");

            usuarioService.actualizarCredenciales(usernameActual, nuevoUsuario, nuevaContrasena);
            return ResponseEntity.ok("Credenciales actualizadas con éxito.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}