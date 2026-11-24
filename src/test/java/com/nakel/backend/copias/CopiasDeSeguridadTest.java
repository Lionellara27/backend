package com.nakel.backend.copias;

import com.nakel.backend.copias.CopiasDeSeguridad.Copia;
import com.nakel.backend.copias.CopiasDeSeguridad.CopiaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las copias de seguridad. No usan Spring ni la base real: cada prueba arma una base chica
 * en una carpeta temporal y usa un reloj que se puede adelantar, para simular semanas en segundos.
 *
 * La base de prueba tiene una tabla "marcas" donde cada día simulado se anota la fecha. Así, mirando
 * qué marcas tiene una copia, se sabe exactamente de qué día es.
 */
class CopiasDeSeguridadTest {

    /** Un reloj al que se le puede cambiar la fecha. */
    static class RelojDePrueba extends Clock {
        private LocalDateTime ahora;

        RelojDePrueba(LocalDateTime ahora) {
            this.ahora = ahora;
        }

        void ponerDia(LocalDate dia) {
            ahora = dia.atTime(9, 0);
        }

        void ponerHora(int hora) {
            ahora = ahora.toLocalDate().atTime(hora, 0);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora.atZone(getZone()).toInstant();
        }
    }

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 23); // un lunes

    @TempDir
    Path temporal;

    Path base;
    Path carpeta;
    RelojDePrueba reloj;
    CopiasDeSeguridad copias;

    @BeforeEach
    void preparar() throws Exception {
        base = temporal.resolve("base.db");
        carpeta = temporal.resolve("Copias");
        reloj = new RelojDePrueba(LUNES.atTime(9, 0));
        copias = new CopiasDeSeguridad(base, carpeta, reloj);
        sql(base, "CREATE TABLE marcas (dia TEXT NOT NULL)", "CREATE TABLE articulos (id INTEGER PRIMARY KEY, nombre TEXT)");
        // Datos "de siempre", para que la base se parezca a una con contenido
        try (Connection c = conectar(base); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            for (int i = 1; i <= 200; i++) s.executeUpdate("INSERT INTO articulos (id, nombre) VALUES (" + i + ", 'articulo " + i + "')");
            c.commit();
        }
    }

    // ----------------------------------------------------------------------------------
    //  Lo básico
    // ----------------------------------------------------------------------------------

    @Test
    void laPrimeraCopiaCreaLaCarpetaYElArchivoDelDia() throws Exception {
        marcar("lunes-1");
        Copia copia = copias.asegurarCopiaDelDia();

        assertNotNull(copia);
        assertEquals("LUNES", copia.dia());
        assertTrue(copia.deHoy());
        assertEquals(List.of("copia-1-lunes.db"), archivosDeCopias());
        assertEquals(List.of("lunes-1"), marcasDe(carpeta.resolve("copia-1-lunes.db")));
        assertEquals(200, contar(carpeta.resolve("copia-1-lunes.db"), "articulos"));
    }

    @Test
    void elMismoDiaLaCopiaAutomaticaSeHaceUnaSolaVez() throws Exception {
        marcar("a-la-mañana");
        assertNotNull(copias.asegurarCopiaDelDia());

        marcar("a-la-tarde");
        reloj.ponerHora(18);
        assertNull(copias.asegurarCopiaDelDia(), "la segunda vez del día no tiene que hacer nada");
        assertTrue(copias.copiaDeHoyHecha());

        // La copia sigue siendo la de la mañana: guarda cómo empezó el día
        assertEquals(List.of("a-la-mañana"), marcasDe(carpeta.resolve("copia-1-lunes.db")));
    }

    @Test
    void elBotonHacerCopiaAhoraSiPisaLaDeHoy() throws Exception {
        marcar("a-la-mañana");
        copias.asegurarCopiaDelDia();
        marcar("a-la-tarde");

        copias.hacerCopiaAhora();

        assertEquals(List.of("a-la-mañana", "a-la-tarde"), marcasDe(carpeta.resolve("copia-1-lunes.db")));
        assertEquals(1, archivosDeCopias().size());
    }

    // ----------------------------------------------------------------------------------
    //  La rotación: un día nuevo pisa el de la semana anterior y nunca hay más de 7
    // ----------------------------------------------------------------------------------

    @Test
    void enLaPrimeraSemanaSeVanSumandoHastaLlegarASiete() throws Exception {
        for (int i = 0; i < 7; i++) {
            LocalDate dia = LUNES.plusDays(i);
            reloj.ponerDia(dia);
            marcar(dia.toString());
            copias.asegurarCopiaDelDia();
            assertEquals(i + 1, archivosDeCopias().size(), "después del día " + (i + 1));
        }
        assertEquals(List.of("copia-1-lunes.db", "copia-2-martes.db", "copia-3-miercoles.db", "copia-4-jueves.db",
                "copia-5-viernes.db", "copia-6-sabado.db", "copia-7-domingo.db"), archivosDeCopias());
    }

    @Test
    void elLunesSiguientePisaLaCopiaDelLunesAnteriorYNoTocaLasOtras() throws Exception {
        for (int i = 0; i < 7; i++) {
            reloj.ponerDia(LUNES.plusDays(i));
            marcar(LUNES.plusDays(i).toString());
            copias.asegurarCopiaDelDia();
        }
        byte[] martesAntes = Files.readAllBytes(carpeta.resolve("copia-2-martes.db"));
        byte[] domingoAntes = Files.readAllBytes(carpeta.resolve("copia-7-domingo.db"));

        LocalDate otroLunes = LUNES.plusDays(7);
        reloj.ponerDia(otroLunes);
        marcar(otroLunes.toString());
        copias.asegurarCopiaDelDia();

        assertEquals(7, archivosDeCopias().size());
        List<String> enLaDelLunes = marcasDe(carpeta.resolve("copia-1-lunes.db"));
        assertEquals(8, enLaDelLunes.size(), "la copia del lunes ahora tiene todo lo cargado hasta el lunes nuevo");
        assertEquals(otroLunes.toString(), enLaDelLunes.get(7));
        assertArrayEquals(martesAntes, Files.readAllBytes(carpeta.resolve("copia-2-martes.db")), "la del martes no se toca");
        assertArrayEquals(domingoAntes, Files.readAllBytes(carpeta.resolve("copia-7-domingo.db")), "la del domingo no se toca");
    }

    @Test
    void enUnAnioYMedioNuncaHayMasDeSieteYCadaCopiaEsDelDiaQueCorresponde() throws Exception {
        Map<DayOfWeek, LocalDate> ultimaVez = new EnumMap<>(DayOfWeek.class);
        for (int i = 0; i < 550; i++) {
            LocalDate dia = LUNES.plusDays(i);
            reloj.ponerDia(dia);
            marcar(dia.toString());
            assertNotNull(copias.asegurarCopiaDelDia(), "el día " + dia + " tiene que hacer su copia");
            ultimaVez.put(dia.getDayOfWeek(), dia);

            List<String> archivos = archivosDeCopias();
            assertTrue(archivos.size() <= 7, "el día " + dia + " hay " + archivos.size() + " archivos: " + archivos);

            // Cada tanto se revisa TODO: cada copia termina exactamente en el último día de ese día de la semana
            if (i % 37 == 0 || i >= 543) {
                for (Copia copia : copias.listar()) {
                    DayOfWeek diaSemana = DayOfWeek.of(List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO").indexOf(copia.dia()) + 1);
                    List<String> marcas = marcasDe(archivoDe(diaSemana));
                    assertEquals(ultimaVez.get(diaSemana).toString(), marcas.get(marcas.size() - 1), "copia del " + copia.nombre() + " al día " + dia);
                    assertEquals(ultimaVez.get(diaSemana), copia.fecha().toLocalDate());
                }
            }
        }
        assertEquals(7, archivosDeCopias().size());
        assertTrue(Files.notExists(carpeta.resolve("antes-de-restaurar.db")));
        // No quedó ningún archivo suelto de ningún otro tipo
        try (Stream<Path> todo = Files.list(carpeta)) {
            assertEquals(7, todo.count());
        }
    }

    @Test
    void siUnDiaNoSeAbreElLocalEsaCopiaQuedaComoEstaba() throws Exception {
        // Tres semanas sin abrir los domingos (salvo el primero)
        for (int i = 0; i < 21; i++) {
            LocalDate dia = LUNES.plusDays(i);
            if (dia.getDayOfWeek() == DayOfWeek.SUNDAY && i > 6) continue;
            reloj.ponerDia(dia);
            marcar(dia.toString());
            copias.asegurarCopiaDelDia();
        }
        assertEquals(7, archivosDeCopias().size());
        List<String> domingo = marcasDe(carpeta.resolve("copia-7-domingo.db"));
        assertEquals(LUNES.plusDays(6).toString(), domingo.get(domingo.size() - 1), "la del domingo sigue siendo la del primer domingo");
        List<String> sabado = marcasDe(carpeta.resolve("copia-6-sabado.db"));
        assertEquals(LUNES.plusDays(19).toString(), sabado.get(sabado.size() - 1), "la del sábado sí es la del último sábado");
    }

    @Test
    void siElSistemaQuedaAbiertoVariosDiasAlPasarLaMedianocheHaceLaCopiaDelDiaNuevo() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        assertNull(copias.asegurarCopiaDelDia());

        reloj.ponerDia(LUNES.plusDays(1)); // nadie reinició nada: solo pasó el tiempo
        assertFalse(copias.copiaDeHoyHecha());
        marcar("martes");
        assertNotNull(copias.asegurarCopiaDelDia());
        assertEquals(List.of("copia-1-lunes.db", "copia-2-martes.db"), archivosDeCopias());
    }

    @Test
    void laListaVaDeLaMasNuevaALaMasVieja() throws Exception {
        for (int i = 0; i < 10; i++) {
            reloj.ponerDia(LUNES.plusDays(i));
            marcar("d" + i);
            copias.asegurarCopiaDelDia();
        }
        List<Copia> lista = copias.listar();
        assertEquals(7, lista.size());
        assertEquals("MIERCOLES", lista.get(0).dia(), "el día 10 (índice 9) es miércoles");
        assertTrue(lista.get(0).deHoy());
        assertEquals("JUEVES", lista.get(6).dia(), "la más vieja es la del jueves de la primera semana");
        for (int i = 1; i < lista.size(); i++) {
            assertTrue(lista.get(i - 1).fecha().isAfter(lista.get(i).fecha()));
            assertFalse(lista.get(i).deHoy());
        }
    }

    // ----------------------------------------------------------------------------------
    //  Una copia que falla nunca rompe la copia buena anterior
    // ----------------------------------------------------------------------------------

    @Test
    void siLaBaseEstaDaniadaLaCopiaFallaPeroLaAnteriorQuedaIntacta() throws Exception {
        marcar("lunes-bueno");
        copias.asegurarCopiaDelDia();
        byte[] copiaBuena = Files.readAllBytes(carpeta.resolve("copia-1-lunes.db"));

        // Una semana después la base está rota (un archivo con basura)
        reloj.ponerDia(LUNES.plusDays(7));
        Files.write(base, "esto no es una base de datos, es basura".repeat(200).getBytes());

        assertThrows(CopiaException.class, () -> copias.hacerCopiaAhora());
        assertNull(copias.asegurarCopiaDelDia(), "la automática directamente no lo intenta");

        assertArrayEquals(copiaBuena, Files.readAllBytes(carpeta.resolve("copia-1-lunes.db")), "la copia buena del lunes no se tocó");
        assertEquals(List.of("copia-1-lunes.db"), archivosDeCopias());
        assertSinProvisorios();
        assertNotNull(copias.avisoBaseSospechosa());
    }

    @Test
    void siNoEstaLaBaseNoSeCreaNingunaCopiaVacia() {
        CopiasDeSeguridad sinBase = new CopiasDeSeguridad(temporal.resolve("no-existe.db"), carpeta, reloj);
        assertThrows(CopiaException.class, sinBase::hacerCopiaAhora);
        assertTrue(Files.notExists(temporal.resolve("no-existe.db")), "no tiene que crear una base vacía de la nada");
        assertTrue(archivosDeCopias().isEmpty());
    }

    @Test
    void siQuedoUnProvisorioDeUnIntentoCortadoSeLimpiaSolo() throws Exception {
        Files.createDirectories(carpeta);
        Files.write(carpeta.resolve("copia-1-lunes.db.tmp"), new byte[]{1, 2, 3});
        marcar("lunes");

        copias.asegurarCopiaDelDia();

        assertSinProvisorios();
        assertEquals(List.of("lunes"), marcasDe(carpeta.resolve("copia-1-lunes.db")));
    }

    @Test
    void siLaBaseSeVacioLaCopiaAutomaticaNoPisaUnaCopiaBuena() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();

        // Una semana después alguien borró el archivo y el sistema creó una base nueva, vacía
        reloj.ponerDia(LUNES.plusDays(7));
        Files.delete(base);
        sql(base, "CREATE TABLE marcas (dia TEXT NOT NULL)", "CREATE TABLE articulos (id INTEGER PRIMARY KEY, nombre TEXT)");

        assertNotNull(copias.avisoBaseSospechosa());
        assertNull(copias.asegurarCopiaDelDia());
        assertEquals(200, contar(carpeta.resolve("copia-1-lunes.db"), "articulos"), "la copia buena sigue con sus 200 artículos");

        // Y se puede volver atrás
        copias.restaurar("LUNES");
        assertEquals(200, contar(base, "articulos"));
        assertNull(copias.avisoBaseSospechosa());
    }

    @Test
    void siSeBorraronPocosDatosLaCopiaSeHaceNormal() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        reloj.ponerDia(LUNES.plusDays(7));
        sql(base, "DELETE FROM articulos WHERE id > 150"); // se dieron de baja 50 de 200: es uso normal

        assertNull(copias.avisoBaseSospechosa());
        assertNotNull(copias.asegurarCopiaDelDia());
        assertEquals(150, contar(carpeta.resolve("copia-1-lunes.db"), "articulos"));
    }

    // ----------------------------------------------------------------------------------
    //  Copiar mientras el sistema está usando la base
    // ----------------------------------------------------------------------------------

    @Test
    void sePuedeCopiarMientrasHayUnaVentaAMedioGuardarYLoQueNoSeConfirmoNoEntra() throws Exception {
        marcar("confirmado");
        try (Connection otra = conectar(base); Statement s = otra.createStatement()) {
            otra.setAutoCommit(false);
            s.executeUpdate("INSERT INTO marcas (dia) VALUES ('a-medio-guardar')"); // todavía sin confirmar

            copias.hacerCopiaAhora();

            assertEquals(List.of("confirmado"), marcasDe(carpeta.resolve("copia-1-lunes.db")));
            otra.commit();
        }
        assertEquals(List.of("confirmado", "a-medio-guardar"), marcasDe(base));
    }

    @Test
    void funcionaAunqueLaCarpetaTengaApostrofosEspaciosYAcentos() throws Exception {
        Path rara = temporal.resolve("Usuario O'Brien Peña").resolve("Mis Copias");
        Path baseRara = temporal.resolve("Usuario O'Brien Peña").resolve("base de datos.db");
        Files.createDirectories(baseRara.getParent());
        Files.copy(base, baseRara);
        CopiasDeSeguridad otras = new CopiasDeSeguridad(baseRara, rara, reloj);

        otras.hacerCopiaAhora();
        sql(baseRara, "DELETE FROM articulos");
        otras.restaurar("LUNES");

        assertEquals(200, contar(baseRara, "articulos"));
    }

    // ----------------------------------------------------------------------------------
    //  Restaurar
    // ----------------------------------------------------------------------------------

    @Test
    void restaurarVuelveLaBaseAComoEstabaEseDia() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        reloj.ponerDia(LUNES.plusDays(1));
        marcar("martes");
        copias.asegurarCopiaDelDia();
        reloj.ponerDia(LUNES.plusDays(2));
        marcar("miercoles");
        sql(base, "DELETE FROM articulos WHERE id > 10"); // el miércoles alguien borró casi todo por error

        copias.restaurar("MARTES");

        assertEquals(List.of("lunes", "martes"), marcasDe(base));
        assertEquals(200, contar(base, "articulos"));
    }

    @Test
    void antesDeRestaurarGuardaComoEstabaYSePuedeDeshacer() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        reloj.ponerDia(LUNES.plusDays(1));
        marcar("martes");
        marcar("venta-del-martes-a-la-tarde");

        copias.restaurar("LUNES"); // ups, no era lo que quería

        assertEquals(List.of("lunes"), marcasDe(base));
        assertEquals(List.of("lunes", "martes", "venta-del-martes-a-la-tarde"), marcasDe(carpeta.resolve("antes-de-restaurar.db")));
        assertEquals(CopiasDeSeguridad.RESPALDO, copias.listar().get(copias.listar().size() - 1).dia());

        copias.restaurar(CopiasDeSeguridad.RESPALDO); // se arrepiente

        assertEquals(List.of("lunes", "martes", "venta-del-martes-a-la-tarde"), marcasDe(base));
        // y ahora "antes de restaurar" es lo que había un momento antes, o sea la del lunes
        assertEquals(List.of("lunes"), marcasDe(carpeta.resolve("antes-de-restaurar.db")));
    }

    @Test
    void restaurarNoCambiaNingunaDeLasSieteCopias() throws Exception {
        for (int i = 0; i < 7; i++) {
            reloj.ponerDia(LUNES.plusDays(i));
            marcar("d" + i);
            copias.asegurarCopiaDelDia();
        }
        List<byte[]> antes = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) antes.add(Files.readAllBytes(archivoDe(d)));

        copias.restaurar("MIERCOLES");
        copias.restaurar("VIERNES");

        int i = 0;
        for (DayOfWeek d : DayOfWeek.values()) assertArrayEquals(antes.get(i++), Files.readAllBytes(archivoDe(d)), "copia del " + d);
        assertEquals(8, cantidadDeArchivosEnLaCarpeta(), "las 7 de siempre más la de 'antes de restaurar', y nada más");
    }

    @Test
    void restaurarUnDiaQueNoTieneCopiaNoTocaLaBase() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        marcar("despues");
        byte[] antes = Files.readAllBytes(base);

        assertThrows(CopiaException.class, () -> copias.restaurar("JUEVES"));
        assertThrows(CopiaException.class, () -> copias.restaurar("cualquier cosa"));
        assertThrows(CopiaException.class, () -> copias.restaurar(null));
        assertThrows(CopiaException.class, () -> copias.restaurar("../../base"));

        assertArrayEquals(antes, Files.readAllBytes(base));
    }

    @Test
    void restaurarUnaCopiaDaniadaNoTocaLaBase() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        marcar("despues");
        Files.write(carpeta.resolve("copia-1-lunes.db"), "copia rota".repeat(500).getBytes());

        CopiaException error = assertThrows(CopiaException.class, () -> copias.restaurar("LUNES"));

        assertTrue(error.getMessage().contains("dañada"));
        assertEquals(List.of("lunes", "despues"), marcasDe(base));
        assertTrue(Files.notExists(carpeta.resolve("antes-de-restaurar.db")));
    }

    @Test
    void sePuedeRestaurarAunqueHayanBorradoElArchivoDeLaBase() throws Exception {
        marcar("lunes");
        copias.asegurarCopiaDelDia();
        Files.delete(base); // el caso que más miedo da: alguien borró la base

        copias.restaurar("LUNES");

        assertEquals(List.of("lunes"), marcasDe(base));
        assertEquals(200, contar(base, "articulos"));
    }

    @Test
    void despuesDeRestaurarLasCopiasSiguenRotandoNormal() throws Exception {
        for (int i = 0; i < 7; i++) {
            reloj.ponerDia(LUNES.plusDays(i));
            marcar("d" + i);
            copias.asegurarCopiaDelDia();
        }
        copias.restaurar("MARTES");
        for (int i = 7; i < 21; i++) {
            reloj.ponerDia(LUNES.plusDays(i));
            marcar("d" + i);
            assertNotNull(copias.asegurarCopiaDelDia());
            assertEquals(7, archivosDeCopias().size());
        }
        List<String> domingo = marcasDe(carpeta.resolve("copia-7-domingo.db"));
        assertEquals(List.of("d0", "d1"), domingo.subList(0, 2), "arranca de lo restaurado (lunes y martes)");
        assertEquals("d20", domingo.get(domingo.size() - 1));
    }

    // ----------------------------------------------------------------------------------
    //  Ayudas
    // ----------------------------------------------------------------------------------

    private Path archivoDe(DayOfWeek dia) {
        String[] nombres = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};
        return carpeta.resolve("copia-" + dia.getValue() + "-" + nombres[dia.getValue() - 1] + ".db");
    }

    private void marcar(String texto) throws Exception {
        sql(base, "INSERT INTO marcas (dia) VALUES ('" + texto + "')");
    }

    private static Connection conectar(Path archivo) throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + archivo);
    }

    private static void sql(Path archivo, String... sentencias) throws Exception {
        try (Connection c = conectar(archivo); Statement s = c.createStatement()) {
            for (String sentencia : sentencias) s.executeUpdate(sentencia);
        }
    }

    private static List<String> marcasDe(Path archivo) throws Exception {
        List<String> marcas = new ArrayList<>();
        try (Connection c = conectar(archivo); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT dia FROM marcas ORDER BY rowid")) {
            while (r.next()) marcas.add(r.getString(1));
        }
        return marcas;
    }

    private static int contar(Path archivo, String tabla) throws Exception {
        try (Connection c = conectar(archivo); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            r.next();
            return r.getInt(1);
        }
    }

    /** Los archivos de copias diarias que hay, ordenados por nombre. */
    private List<String> archivosDeCopias() {
        if (Files.notExists(carpeta)) return List.of();
        try (Stream<Path> todo = Files.list(carpeta)) {
            return todo.map(p -> p.getFileName().toString()).filter(n -> n.startsWith("copia-") && n.endsWith(".db")).sorted().toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long cantidadDeArchivosEnLaCarpeta() throws Exception {
        try (Stream<Path> todo = Files.list(carpeta)) {
            return todo.count();
        }
    }

    private void assertSinProvisorios() throws Exception {
        try (Stream<Path> todo = Files.list(carpeta)) {
            List<String> sobrantes = todo.map(p -> p.getFileName().toString()).filter(n -> !n.endsWith(".db")).toList();
            assertTrue(sobrantes.isEmpty(), "quedaron archivos provisorios: " + sobrantes);
        }
    }
}
