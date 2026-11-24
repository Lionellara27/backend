package com.nakel.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Arreglos que se hacen solos cada vez que arranca el sistema, para que una base vieja quede usable
 * sin tener que correr nada a mano.
 *
 * Hoy hace una sola cosa: recrea los artículos que fueron BORRADOS de verdad en versiones anteriores
 * (antes de que existiera la baja lógica) pero que siguen figurando en ventas o cambios viejos.
 * Sin esto, al abrir el Historial de ventas el sistema buscaba un artículo que no existe y daba error.
 * Se recrean como dados de baja: no aparecen en el Catálogo ni en el Mostrador, y no tienen stock.
 *
 * Es seguro correrlo siempre: si no falta ningún artículo, no hace nada.
 */
@Component
public class ReparadorDeBase implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public ReparadorDeBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int recreados = jdbc.update("""
                    INSERT INTO articulos (id, codigo, nombre, origen, precio, stock_actual, stock_minimo, alicuota_iva, categoria_id, material_id, activo)
                    SELECT f.id, 'ELIMINADO-' || f.id, 'Articulo eliminado (' || f.id || ')', 'REVENTA', 0, 0, 0, 21.0, NULL, NULL, 0
                    FROM (SELECT articulo_id AS id FROM detalles_venta UNION SELECT articulo_id FROM items_cambio) f
                    WHERE f.id IS NOT NULL AND f.id NOT IN (SELECT id FROM articulos)
                    """);
            if (recreados > 0) {
                System.out.println("Reparación de la base: se recrearon " + recreados
                        + " artículo(s) borrados que seguían figurando en ventas viejas.");
            }
        } catch (Exception e) {
            // No tiene que impedir que el sistema arranque: se avisa y se sigue
            System.err.println("No se pudo revisar la base al arrancar: " + e.getMessage());
        }
    }
}
