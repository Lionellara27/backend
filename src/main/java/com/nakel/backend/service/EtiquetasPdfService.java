package com.nakel.backend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

/**
 * Etiquetas con código de barras para pegar o colgar en los artículos.
 *
 * Cada etiqueta lleva el nombre, el código del artículo en barras (formato Code 128, que admite números,
 * letras y guiones, así que sirve tanto para el SKU propio "1004-0005" como para un código de fábrica)
 * y, si se pide, el precio.
 *
 * La hoja es A4 con 24 etiquetas: 3 columnas x 8 filas de 70 x 37 mm, que es la medida estándar del
 * papel autoadhesivo. También se puede imprimir en papel común y recortar por las líneas.
 */
@Service
public class EtiquetasPdfService {

    public static final int MAXIMO_ETIQUETAS = 2400; // 100 hojas: más que eso seguro es un error
    private static final int COLUMNAS = 3;
    private static final int FILAS = 8;
    private static final float MM = 72f / 25.4f; // puntos por milímetro
    private static final float ANCHO_ETIQUETA = 70 * MM;
    private static final float ALTO_ETIQUETA = 37.125f * MM; // 297 mm / 8

    private final ArticuloRepository articuloRepository;

    public EtiquetasPdfService(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    /**
     * @param idArticulo   si viene, solo ese artículo
     * @param categoriaId  si viene, solo los de esa categoría
     * @param soloConStock true = se saltean los que están en 0
     * @param copias       "UNA" (una por artículo), "STOCK" (una por cada unidad en stock) o "FIJA"
     * @param cantidad     cuántas de cada uno cuando copias = "FIJA"
     * @param conPrecio    si la etiqueta muestra el precio
     */
    @Transactional(readOnly = true)
    public byte[] generar(Long idArticulo, Long categoriaId, boolean soloConStock,
                          String copias, Integer cantidad, boolean conPrecio) {

        String modo = copias == null ? "UNA" : copias.trim().toUpperCase();
        if ("FIJA".equals(modo) && (cantidad == null || cantidad < 1)) {
            throw new IllegalArgumentException("Indicá cuántas etiquetas querés de cada artículo.");
        }

        // 1. Qué artículos entran
        List<Articulo> elegidos = new ArrayList<>();
        for (Articulo art : articuloRepository.findAllOptimizado()) { // solo los activos
            if (idArticulo != null && !idArticulo.equals(art.getId())) continue;
            if (categoriaId != null && (art.getCategoria() == null || !categoriaId.equals(art.getCategoria().getId()))) continue;
            int stock = art.getStockActual() != null ? art.getStockActual() : 0;
            if (soloConStock && stock <= 0) continue;
            elegidos.add(art);
        }
        if (elegidos.isEmpty()) {
            throw new IllegalArgumentException("No hay artículos para imprimir con esa selección.");
        }

        // 2. Cuántas etiquetas de cada uno
        List<Articulo> etiquetas = new ArrayList<>();
        List<String> sinEtiqueta = new ArrayList<>();
        for (Articulo art : elegidos) {
            if (!sePuedeCodificar(art.getCodigo())) {
                sinEtiqueta.add(art.getCodigo());
                continue;
            }
            int stock = art.getStockActual() != null ? art.getStockActual() : 0;
            int n = switch (modo) {
                case "STOCK" -> Math.max(stock, 0);
                case "FIJA" -> cantidad;
                default -> 1;
            };
            for (int i = 0; i < n; i++) etiquetas.add(art);
            if (etiquetas.size() > MAXIMO_ETIQUETAS) {
                throw new IllegalArgumentException("Son más de " + MAXIMO_ETIQUETAS + " etiquetas (" + (MAXIMO_ETIQUETAS / (COLUMNAS * FILAS))
                        + " hojas). Elegí una categoría o menos copias por artículo.");
            }
        }
        if (etiquetas.isEmpty()) {
            throw new IllegalArgumentException(sinEtiqueta.isEmpty()
                    ? "Con esa selección no sale ninguna etiqueta (los artículos elegidos no tienen stock)."
                    : "El código de esos artículos tiene caracteres que no se pueden poner en barras: " + String.join(", ", sinEtiqueta));
        }

        return armarPdf(etiquetas, conPrecio);
    }

    /** Code 128 admite los caracteres ASCII imprimibles; una "ñ" o un acento no se pueden codificar. */
    private boolean sePuedeCodificar(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.length() > 24) return false;
        for (char c : codigo.toCharArray()) {
            if (c < 32 || c > 126) return false;
        }
        return true;
    }

    private byte[] armarPdf(List<Articulo> etiquetas, boolean conPrecio) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sin márgenes: las 24 etiquetas ocupan la hoja A4 exacta (como el papel autoadhesivo de 70 x 37 mm)
            Document document = new Document(PageSize.A4, 0, 0, 0, 0);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte lienzo = writer.getDirectContent();

            Font fuenteNombre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.BLACK);
            Font fuentePrecio = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f, Color.BLACK);
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setGroupingSeparator('.');
            simbolos.setDecimalSeparator(',');
            DecimalFormat pesos = new DecimalFormat("$ #,##0.##", simbolos);

            PdfPTable tabla = nuevaTabla();
            int enLaHoja = 0;
            for (Articulo art : etiquetas) {
                tabla.addCell(celda(art, conPrecio, lienzo, fuenteNombre, fuentePrecio, pesos));
                enLaHoja++;
                if (enLaHoja == COLUMNAS * FILAS) { // hoja llena
                    document.add(tabla);
                    document.newPage();
                    tabla = nuevaTabla();
                    enLaHoja = 0;
                }
            }
            if (enLaHoja > 0) {
                // Se completa la última fila con etiquetas vacías para que la tabla cierre bien
                while (enLaHoja % COLUMNAS != 0) {
                    tabla.addCell(celdaVacia());
                    enLaHoja++;
                }
                document.add(tabla);
            }

            document.close();
            return out.toByteArray();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al generar las etiquetas", e);
        }
    }

    private PdfPTable nuevaTabla() {
        PdfPTable tabla = new PdfPTable(COLUMNAS);
        tabla.setTotalWidth(ANCHO_ETIQUETA * COLUMNAS);
        tabla.setLockedWidth(true);
        tabla.setSpacingBefore(0);
        tabla.setSpacingAfter(0);
        return tabla;
    }

    private PdfPCell celda(Articulo art, boolean conPrecio, PdfContentByte lienzo,
                           Font fuenteNombre, Font fuentePrecio, DecimalFormat pesos) {
        PdfPCell celda = celdaVacia();

        // Nombre (si es muy largo se corta para que no empuje al código fuera de la etiqueta)
        String nombre = art.getNombre() == null ? "" : art.getNombre().trim();
        if (nombre.length() > 34) nombre = nombre.substring(0, 33) + "…";
        PdfPTable interior = new PdfPTable(1);
        interior.setWidthPercentage(100);

        PdfPCell cNombre = new PdfPCell(new Phrase(nombre, fuenteNombre));
        cNombre.setBorder(Rectangle.NO_BORDER);
        cNombre.setHorizontalAlignment(Element.ALIGN_CENTER);
        cNombre.setPaddingBottom(2);
        interior.addCell(cNombre);

        // Código en barras, con el código legible debajo
        Barcode128 barras = new Barcode128();
        barras.setCode(art.getCodigo().trim());
        barras.setCodeType(Barcode128.CODE128);
        barras.setBarHeight(11 * MM);
        barras.setX(0.9f);      // ancho de la barra más fina, en puntos (0,32 mm): cómodo para cualquier lector
        barras.setSize(7.5f);   // tamaño del texto de abajo
        barras.setBaseline(8f);
        Image imagen = barras.createImageWithBarcode(lienzo, Color.BLACK, Color.BLACK);
        // Si el código es largo, se achica para que entre dejando margen blanco a los costados (el lector lo necesita)
        float anchoMaximo = ANCHO_ETIQUETA - 12 * MM;
        if (imagen.getWidth() > anchoMaximo) imagen.scaleToFit(anchoMaximo, 100);

        PdfPCell cBarras = new PdfPCell(imagen, false);
        cBarras.setBorder(Rectangle.NO_BORDER);
        cBarras.setHorizontalAlignment(Element.ALIGN_CENTER);
        cBarras.setPaddingTop(1);
        cBarras.setPaddingBottom(conPrecio ? 2 : 0);
        interior.addCell(cBarras);

        if (conPrecio && art.getPrecio() != null) {
            PdfPCell cPrecio = new PdfPCell(new Phrase(pesos.format(art.getPrecio()), fuentePrecio));
            cPrecio.setBorder(Rectangle.NO_BORDER);
            cPrecio.setHorizontalAlignment(Element.ALIGN_CENTER);
            cPrecio.setPadding(0);
            interior.addCell(cPrecio);
        }

        celda.addElement(interior);
        return celda;
    }

    private PdfPCell celdaVacia() {
        PdfPCell celda = new PdfPCell();
        celda.setFixedHeight(ALTO_ETIQUETA);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celda.setPadding(3 * MM);
        // Línea gris muy suave: guía para recortar en papel común, casi invisible en papel autoadhesivo
        celda.setBorder(Rectangle.BOX);
        celda.setBorderColor(new Color(215, 215, 215));
        celda.setBorderWidth(0.3f);
        return celda;
    }
}
