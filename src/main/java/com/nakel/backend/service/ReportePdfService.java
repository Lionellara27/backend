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

    public byte[] generarReporteStock() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Creamos el documento tamaño A4
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Paragraph titulo = new Paragraph("Reporte de Stock de Artículos", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // Buscamos todos los artículos ACTIVOS (gracias al findAllOptimizado)
            List<Articulo> todosLosArticulos = articuloRepository.findAllOptimizado();

            // Creamos la tabla con 4 columnas
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 5f, 2f, 2f}); // Anchos de las columnas

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

            // Llenamos las filas solo con artículos que tengan stock > 0
            Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
            for (Articulo art : todosLosArticulos) {
                if (art.getStockActual() != null && art.getStockActual() > 0) {
                    table.addCell(new PdfPCell(new Phrase(art.getCodigo(), fontFila)));
                    table.addCell(new PdfPCell(new Phrase(art.getNombre(), fontFila)));
                    table.addCell(new PdfPCell(new Phrase("$" + art.getPrecio(), fontFila)));

                    PdfPCell cellStock = new PdfPCell(new Phrase(String.valueOf(art.getStockActual()), fontFila));
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