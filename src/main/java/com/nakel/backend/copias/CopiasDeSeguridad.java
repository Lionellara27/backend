package com.nakel.backend.copias;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Copias de seguridad de la base de datos: UNA POR DÍA DE LA SEMANA, que se va pisando.
 *
 * Siempre hay como mucho 7 archivos (copia-1-lunes ... copia-7-domingo). El martes se pisa la copia
 * del martes anterior, el miércoles la del miércoles anterior, y así: nunca se acumulan, y siempre
 * se puede volver a cualquiera de los últimos 7 días en que se abrió el sistema.
 *
 * Cuidados que tiene esta clase (cada uno tiene su prueba en CopiasDeSeguridadTest):
 *  - La copia nueva se arma primero en un archivo aparte, se REVISA, y recién entonces reemplaza a la
 *    vieja. Si algo falla en el medio, la copia buena anterior queda intacta.
 *  - La copia automática se hace una sola vez por día (la primera vez que se abre el sistema), así
 *    guarda cómo estaba todo al terminar el día anterior.
 *  - Si la base actual tiene muchísimos menos datos que la última copia (por ejemplo, porque alguien
 *    borró el archivo y el sistema creó uno vacío), la copia automática NO se hace: no se pisa una
 *    copia buena con una base vacía.
 *  - Antes de restaurar se guarda cómo estaba la base en ese momento ("antes de restaurar"), por si
 *    se eligió el día equivocado. Es el único archivo extra además de los 7.
 *
 * No depende de Spring ni del resto del sistema: recibe dónde está la base, dónde van las copias y
 * un reloj (que en las pruebas se adelanta para simular semanas enteras).
 */
public class CopiasDeSeguridad {

    /** Clave de la copia que se guarda sola justo antes de restaurar. */
    public static final String RESPALDO = "ANTES_DE_RESTAURAR";

    private static final String[] CLAVES = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};
    private static final String[] NOMBRES = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String ARCHIVO_RESPALDO = "antes-de-restaurar.db";

    /** Una copia guardada: de qué día es, cuándo se hizo y cuánto pesa. */
    public record Copia(String dia, String nombre, LocalDateTime fecha, long bytes, boolean deHoy) {
    }

    /** Un problema que se le puede mostrar tal cual a quien usa el sistema. */
    public static class CopiaException extends RuntimeException {
        public CopiaException(String mensaje) {
            super(mensaje);
        }

        public CopiaException(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    private final Path base;
    private final Path carpeta;
    private final Clock reloj;

    public CopiasDeSeguridad(Path base, Path carpeta, Clock reloj) {
        this.base = base.toAbsolutePath().normalize();
        this.carpeta = carpeta.toAbsolutePath().normalize();
        this.reloj = reloj;
    }

    public Path getCarpeta() {
        return carpeta;
    }

    public Path getBase() {
        return base;
    }

    // =====================================================================================
    //  HACER COPIAS
    // =====================================================================================

    /**
     * La copia automática del día. Si hoy ya se hizo, no hace nada (devuelve null).
     * Tampoco la hace si la base actual parece vacía comparada con la última copia: ver {@link #avisoBaseSospechosa()}.
     */
    public synchronized Copia asegurarCopiaDelDia() {
        if (copiaDeHoyHecha()) return null;
        if (avisoBaseSospechosa() != null) return null;
        return hacerCopiaAhora();
    }

    /** Hace la copia de hoy sí o sí, pisando la que hubiera de este día de la semana. */
    public synchronized Copia hacerCopiaAhora() {
        DayOfWeek hoy = LocalDate.now(reloj).getDayOfWeek();
        Path destino = archivoDe(hoy);
        copiarBaseA(destino);
        return leer(CLAVES[hoy.getValue() - 1], NOMBRES[hoy.getValue() - 1], destino);
    }

    /** ¿Ya está hecha la copia de hoy? (Existe la de este día de la semana y es de la fecha de hoy.) */
    public synchronized boolean copiaDeHoyHecha() {
        LocalDate hoy = LocalDate.now(reloj);
        Path archivo = archivoDe(hoy.getDayOfWeek());
        return Files.isRegularFile(archivo) && fechaDe(archivo).toLocalDate().equals(hoy);
    }

    /**
     * Si la base actual tiene muchísimos menos datos que la copia más nueva, devuelve un texto que lo explica;
     * si está todo normal, null. Sirve para no pisar una copia buena con una base que se vació.
     */
    public synchronized String avisoBaseSospechosa() {
        Copia ultima = listar().stream().filter(c -> !RESPALDO.equals(c.dia())).findFirst().orElse(null);
        if (ultima == null) return null;
        try {
            long enLaCopia = contarFilas(archivoDe(ultima.dia()));
            if (enLaCopia < 50) return null; // con tan pocos datos no se puede sacar ninguna conclusión
            if (!Files.isRegularFile(base)) {
                return "No se encuentra la base de datos del sistema, pero hay copias de seguridad guardadas. "
                        + "Se puede volver a la última copia.";
            }
            long ahora = contarFilas(base);
            if (ahora * 5 < enLaCopia) {
                return "La base de datos tiene muchos menos datos que la última copia de seguridad ("
                        + ahora + " registros contra " + enLaCopia + "). No se hizo la copia automática de hoy "
                        + "para no pisar una copia buena. Si se perdieron datos, se puede volver a la última copia.";
            }
            return null;
        } catch (Exception e) {
            return "No se pudo leer la base de datos del sistema: puede estar dañada. No se hizo la copia automática "
                    + "de hoy para no pisar una copia buena. Se puede volver a la última copia.";
        }
    }

    // =====================================================================================
    //  LISTAR
    // =====================================================================================

    /** Las copias que hay, de la más nueva a la más vieja; la de "antes de restaurar" va al final. */
    public synchronized List<Copia> listar() {
        List<Copia> copias = new ArrayList<>();
        for (DayOfWeek dia : DayOfWeek.values()) {
            Path archivo = archivoDe(dia);
            if (Files.isRegularFile(archivo)) {
                copias.add(leer(CLAVES[dia.getValue() - 1], NOMBRES[dia.getValue() - 1], archivo));
            }
        }
        copias.sort(Comparator.comparing(Copia::fecha).reversed());
        Path respaldo = carpeta.resolve(ARCHIVO_RESPALDO);
        if (Files.isRegularFile(respaldo)) {
            copias.add(leer(RESPALDO, "Como estaba antes de la última restauración", respaldo));
        }
        return copias;
    }

    // =====================================================================================
    //  RESTAURAR
    // =====================================================================================

    /**
     * Vuelve la base a como estaba en la copia elegida (LUNES ... DOMINGO, o ANTES_DE_RESTAURAR).
     * Antes guarda cómo está ahora, por si se eligió mal.
     */
    public synchronized void restaurar(String dia) {
        Path origen = archivoDe(dia);
        if (origen == null || !Files.isRegularFile(origen)) {
            throw new CopiaException("No hay una copia guardada de ese día.");
        }
        try {
            revisar(origen);
        } catch (Exception e) {
            throw new CopiaException("Esa copia está dañada y no se puede usar. La base de datos no se tocó.", e);
        }

        // 1. Red de seguridad: cómo está la base AHORA, en un archivo provisorio
        Path respaldo = carpeta.resolve(ARCHIVO_RESPALDO);
        Path respaldoNuevo = carpeta.resolve(ARCHIVO_RESPALDO + ".nuevo");
        boolean hayRespaldoNuevo = false;
        try {
            Files.deleteIfExists(respaldoNuevo);
            if (Files.isRegularFile(base) && Files.size(base) > 0) {
                try {
                    copiarBaseA(respaldoNuevo);
                } catch (Exception baseDaniada) {
                    // Si la base actual está tan rota que no se puede copiar ordenadamente, se guarda el archivo tal cual
                    Files.copy(base, respaldoNuevo, StandardCopyOption.REPLACE_EXISTING);
                }
                hayRespaldoNuevo = true;
            }
        } catch (Exception e) {
            throw new CopiaException("No se pudo guardar cómo está la base ahora, así que no se restauró nada.", e);
        }

        // 2. Restaurar. SQLite lo hace de forma segura: si se corta en el medio, la base vuelve a como estaba.
        try (Connection conexion = abrir(base, false); Statement sentencia = conexion.createStatement()) {
            sentencia.executeUpdate("restore from \"" + origen + "\"");
        } catch (Exception e) {
            borrar(respaldoNuevo);
            throw new CopiaException("No se pudo restaurar la copia: " + e.getMessage(), e);
        }

        try {
            revisar(base);
        } catch (Exception e) {
            throw new CopiaException("La copia se restauró pero la base no quedó bien. "
                    + "Cómo estaba antes quedó guardado y se puede recuperar.", e);
        } finally {
            // 3. Recién ahora el provisorio pasa a ser la copia "antes de restaurar"
            if (hayRespaldoNuevo) {
                try {
                    mover(respaldoNuevo, respaldo);
                    Files.setLastModifiedTime(respaldo, FileTime.from(reloj.instant()));
                } catch (Exception ignorada) {
                    // la restauración ya está hecha; perder la red de seguridad no la deshace
                }
            }
        }
    }

    // =====================================================================================
    //  Por dentro
    // =====================================================================================

    /** Copia la base a un archivo: primero a un provisorio, se revisa, y recién entonces reemplaza al destino. */
    private void copiarBaseA(Path destino) {
        Path provisorio = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            if (!Files.isRegularFile(base) || Files.size(base) == 0) {
                throw new CopiaException("No se encontró la base de datos del sistema.");
            }
            Files.createDirectories(carpeta);
            Files.deleteIfExists(provisorio); // por si quedó uno de un intento anterior que se cortó

            int tablasOriginal;
            try (Connection conexion = abrir(base, false); Statement sentencia = conexion.createStatement()) {
                tablasOriginal = contarObjetos(sentencia);
                // VACUUM INTO saca una "foto" completa y ordenada de la base aunque el sistema la esté usando
                sentencia.execute("VACUUM INTO '" + provisorio.toString().replace("'", "''") + "'");
            }

            int tablasCopia = revisar(provisorio);
            if (tablasCopia != tablasOriginal) {
                throw new CopiaException("La copia salió incompleta y se descartó.");
            }

            mover(provisorio, destino);
            Files.setLastModifiedTime(destino, FileTime.from(reloj.instant()));
        } catch (CopiaException e) {
            borrar(provisorio);
            throw e;
        } catch (Exception e) {
            borrar(provisorio);
            throw new CopiaException("No se pudo hacer la copia de seguridad: " + e.getMessage(), e);
        }
    }

    /** Abre el archivo y le pide a SQLite que se revise entero. Devuelve cuántas tablas e índices tiene. */
    private int revisar(Path archivo) throws Exception {
        if (!Files.isRegularFile(archivo) || Files.size(archivo) == 0) {
            throw new CopiaException("El archivo está vacío.");
        }
        try (Connection conexion = abrir(archivo, true); Statement sentencia = conexion.createStatement()) {
            try (ResultSet r = sentencia.executeQuery("PRAGMA integrity_check")) {
                if (!r.next() || !"ok".equalsIgnoreCase(r.getString(1))) {
                    throw new CopiaException("El archivo está dañado.");
                }
            }
            return contarObjetos(sentencia);
        }
    }

    private static int contarObjetos(Statement sentencia) throws Exception {
        try (ResultSet r = sentencia.executeQuery("SELECT COUNT(*) FROM sqlite_master")) {
            r.next();
            return r.getInt(1);
        }
    }

    /** Cuántas filas hay en total sumando todas las tablas: una medida simple de "cuántos datos tiene". */
    private long contarFilas(Path archivo) throws Exception {
        long total = 0;
        try (Connection conexion = abrir(archivo, true); Statement sentencia = conexion.createStatement()) {
            List<String> tablas = new ArrayList<>();
            try (ResultSet r = sentencia.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
                while (r.next()) tablas.add(r.getString(1));
            }
            for (String tabla : tablas) {
                try (ResultSet r = sentencia.executeQuery("SELECT COUNT(*) FROM \"" + tabla.replace("\"", "\"\"") + "\"")) {
                    r.next();
                    total += r.getLong(1);
                }
            }
        }
        return total;
    }

    private static Connection abrir(Path archivo, boolean soloLectura) throws Exception {
        SQLiteConfig configuracion = new SQLiteConfig();
        configuracion.setBusyTimeout(8000); // si el sistema está guardando algo justo ahora, espera en vez de fallar
        configuracion.setReadOnly(soloLectura);
        SQLiteDataSource origen = new SQLiteDataSource(configuracion);
        origen.setUrl("jdbc:sqlite:" + archivo);
        return origen.getConnection();
    }

    private static void mover(Path desde, Path hasta) throws Exception {
        try {
            Files.move(desde, hasta, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(desde, hasta, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (Exception ignorada) {
            // se vuelve a intentar la próxima vez
        }
    }

    private Path archivoDe(DayOfWeek dia) {
        int n = dia.getValue(); // 1 = lunes ... 7 = domingo
        String nombre = java.text.Normalizer.normalize(NOMBRES[n - 1].toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return carpeta.resolve("copia-" + n + "-" + nombre + ".db");
    }

    private Path archivoDe(String clave) {
        if (clave == null) return null;
        if (RESPALDO.equalsIgnoreCase(clave.trim())) return carpeta.resolve(ARCHIVO_RESPALDO);
        for (int i = 0; i < CLAVES.length; i++) {
            if (CLAVES[i].equalsIgnoreCase(clave.trim())) return archivoDe(DayOfWeek.of(i + 1));
        }
        return null;
    }

    private LocalDateTime fechaDe(Path archivo) {
        try {
            return LocalDateTime.ofInstant(Files.getLastModifiedTime(archivo).toInstant(), reloj.getZone());
        } catch (Exception e) {
            return LocalDateTime.MIN;
        }
    }

    private Copia leer(String clave, String nombre, Path archivo) {
        long bytes;
        try {
            bytes = Files.size(archivo);
        } catch (Exception e) {
            bytes = 0;
        }
        LocalDateTime fecha = fechaDe(archivo);
        return new Copia(clave, nombre, fecha, bytes, fecha.toLocalDate().equals(LocalDate.now(reloj)));
    }
}
