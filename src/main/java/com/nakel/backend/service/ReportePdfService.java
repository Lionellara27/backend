package com.nakel.backend.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nakel.backend.model.Articulo;
import com.nakel.backend.repository.ArticuloRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.awt.Color;

@Service
public class ReportePdfService {

    private final ArticuloRepository articuloRepository;

    public ReportePdfService(ArticuloRepository articuloRepository) {
        this.articuloRepository = articuloRepository;
    }

    /**
     * @param mayorA qué artículos entran en el reporte:
     *               null = todos (también los que están en 0);
     *               0    = solo los que tienen stock;
     *               N    = solo los que tienen MÁS de N unidades.
     */
    public byte[] generarReporteStock(Integer mayorA) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Creamos el documento tamaño A4
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph titulo = new Paragraph("Reporte de Stock de Artículos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(6);
            document.add(titulo);

            // Buscamos todos los artículos ACTIVOS (gracias al findAllOptimizado)
            List<Articulo> todosLosArticulos = articuloRepository.findAllOptimizado();

            // Creamos la tabla con 4 columnas (el encabezado se repite en cada hoja)
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 5f, 2f, 2f}); // Anchos de las columnas
            table.setHeaderRows(1);

            // Cabeceras de la tabla
            String[] cabeceras = {"Código", "Nombre", "Precio", "Stock"};
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            for (String cabecera : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cabecera, fontCabecera));
                cell.setBackgroundColor(Color.DARK_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Qué artículos entran, según lo elegido en la pantalla
            List<Articulo> incluidos = new java.util.ArrayList<>();
            for (Articulo art : todosLosArticulos) {
                int stock = art.getStockActual() != null ? art.getStockActual() : 0;
                if (mayorA == null || stock > mayorA) incluidos.add(art);
            }

            // Debajo del título: qué se imprimió, cuántos son y la fecha
            String criterio = mayorA == null ? "Todos los artículos"
                    : mayorA == 0 ? "Solo artículos con stock"
                    : "Artículos con más de " + mayorA + " unidades";
            Paragraph subtitulo = new Paragraph(criterio + "  ·  " + incluidos.size() + " artículos  ·  "
                    + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY));
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(16);
            document.add(subtitulo);

            Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

            // Mismo formato de plata que las pantallas: $ 10.000,00
            java.text.DecimalFormatSymbols simbolos = new java.text.DecimalFormatSymbols();
            simbolos.setGroupingSeparator('.');
            simbolos.setDecimalSeparator(',');
            java.text.DecimalFormat pesos = new java.text.DecimalFormat("$ #,##0.00", simbolos);

            for (Articulo art : incluidos) {
                {
                    table.addCell(new PdfPCell(new Phrase(art.getCodigo(), fontFila)));
                    table.addCell(new PdfPCell(new Phrase(art.getNombre(), fontFila)));
                    PdfPCell cellPrecio = new PdfPCell(new Phrase(
                            art.getPrecio() != null ? pesos.format(art.getPrecio()) : "-", fontFila));
                    cellPrecio.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellPrecio);

                    PdfPCell cellStock = new PdfPCell(new Phrase(String.valueOf(art.getStockActual() != null ? art.getStockActual() : 0), fontFila));
                    cellStock.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cellStock);
                }
            }

            document.add(table);
            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF de stock", e);
        }
    }
}