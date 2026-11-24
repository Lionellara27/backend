package com.nakel.backend.copias;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;

/**
 * Engancha las copias de seguridad con el sistema:
 *  - al arrancar hace la copia del día (si todavía no se hizo hoy), ANTES de cualquier otra tarea de arranque;
 *  - y vuelve a fijarse cada media hora, por si la computadora queda prendida de un día para el otro.
 *
 * Dónde se guardan: en una carpeta de sistema que no está a la vista
 * (C:\Usuarios\...\AppData\Local\NakelSoftware\Copias), para que nadie las borre sin querer.
 * Si el sistema se levanta sobre OTRA base (por ejemplo, una copia para probar), las copias van a una
 * carpeta al lado de esa base, así nunca se mezclan con las del local.
 */
@Configuration
@EnableScheduling
public class CopiasAutomaticas {

    private CopiasDeSeguridad copias;

    @Bean
    public CopiasDeSeguridad copiasDeSeguridad(@Value("${spring.datasource.url}") String urlBase,
                                               @Value("${nakel.copias.carpeta:}") String carpetaElegida) {
        Path base = rutaDeLaBase(urlBase);
        Path carpeta = (carpetaElegida != null && !carpetaElegida.isBlank())
                ? Paths.get(carpetaElegida.trim())
                : carpetaPorDefecto(base);
        copias = new CopiasDeSeguridad(base, carpeta, Clock.systemDefaultZone());
        return copias;
    }

    /** "jdbc:sqlite:C:/.../base_datos_nakel.db?algo=1" -> la ruta del archivo. */
    static Path rutaDeLaBase(String url) {
        String ruta = url.trim();
        if (ruta.toLowerCase().startsWith("jdbc:sqlite:")) ruta = ruta.substring("jdbc:sqlite:".length());
        int parametros = ruta.indexOf('?');
        if (parametros >= 0) ruta = ruta.substring(0, parametros);
        return Paths.get(ruta).toAbsolutePath().normalize();
    }

    static Path carpetaPorDefecto(Path base) {
        String casa = System.getProperty("user.home");
        Path baseDelLocal = Paths.get(casa, "Documents", "NakelSoftware", "base_datos_nakel.db").toAbsolutePath().normalize();
        if (base.toString().equalsIgnoreCase(baseDelLocal.toString())) {
            return Paths.get(casa, "AppData", "Local", "NakelSoftware", "Copias");
        }
        return base.resolveSibling(base.getFileName() + "-copias");
    }

    /** Al arrancar, primero que nada: así la copia guarda cómo quedó todo la última vez que se usó. */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ApplicationRunner copiaAlArrancar(CopiasDeSeguridad copias) {
        return (ApplicationArguments args) -> copiarSiHaceFalta(copias);
    }

    /** Por si la computadora queda prendida varios días sin cerrar el sistema. */
    @Scheduled(initialDelay = 30 * 60 * 1000L, fixedDelay = 30 * 60 * 1000L)
    public void revisarCadaMediaHora() {
        if (copias != null) copiarSiHaceFalta(copias);
    }

    private static void copiarSiHaceFalta(CopiasDeSeguridad copias) {
        try {
            CopiasDeSeguridad.Copia hecha = copias.asegurarCopiaDelDia();
            if (hecha != null) {
                System.out.println("Copia de seguridad del día hecha (" + hecha.nombre() + ") en " + copias.getCarpeta());
            } else {
                String aviso = copias.avisoBaseSospechosa();
                if (aviso != null) System.err.println("COPIAS DE SEGURIDAD: " + aviso);
            }
        } catch (Exception e) {
            // Una copia que falla nunca tiene que impedir que el sistema arranque o siga andando
            System.err.println("No se pudo hacer la copia de seguridad del día: " + e.getMessage());
        }
    }
}
