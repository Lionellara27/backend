package com.nakel.backend.service;

import com.nakel.backend.model.DetalleVenta;
import com.nakel.backend.model.Venta;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class ImpresionTicketService {

    /**
     * Genera el texto del ticket listo para enviar a la comandera térmica.
     *
     * @param venta La venta original que se va a imprimir.
     * @param esTicketCambio true si es "Ticket de Regalo/Cambio" (oculta precios).
     * @return El string con el diseño del ticket.
     */
    public String generarTicketVenta(Venta venta, boolean esTicketCambio) {
        StringBuilder ticket = new StringBuilder();

        // 1. Cabecera del Local
        ticket.append("========================================\n");
        ticket.append("             N A K E L                  \n");
        ticket.append("========================================\n");

        // Formatear fecha si es necesario (asumiendo que getFecha() devuelve LocalDateTime)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaFormat = venta.getFechaHora() != null ? venta.getFechaHora().format(formatter) : "S/F";

        ticket.append("Fecha: ").append(fechaFormat).append("\n");
        ticket.append("Ticket Nro: ").append(venta.getId()).append("\n");
        ticket.append("----------------------------------------\n");

        // 2. Encabezado de Columnas según el tipo de ticket
        if (esTicketCambio) {
            ticket.append("           TICKET DE CAMBIO             \n");
            ticket.append("----------------------------------------\n");
            ticket.append("CANT | DESCRIPCIÓN\n");
        } else {
            ticket.append("CANT | DESCRIPCIÓN        | P.UNIT | SUB\n");
        }
        ticket.append("----------------------------------------\n");

        // 3. Iterar los artículos de la venta
        if (venta.getDetalles() != null) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                String nombreArt = detalle.getArticulo().getNombre();
                int cant = detalle.getCantidad();

                if (esTicketCambio) {
                    // MODO REGALO: Solo cantidad y nombre
                    ticket.append(String.format("%-4d | %s\n", cant, nombreArt));
                } else {
                    // MODO NORMAL: Cantidad, nombre, precio y subtotal
                    double precio = detalle.getPrecioUnitario().doubleValue();
                    double subtotal = precio * cant;
                    // Recortamos el nombre si es muy largo para que no rompa el ticket
                    String nombreCorto = nombreArt.length() > 18 ? nombreArt.substring(0, 15) + "..." : nombreArt;
                    ticket.append(String.format("%-4d | %-18s | $%.2f | $%.2f\n", cant, nombreCorto, precio, subtotal));
                }
            }
        }

        ticket.append("----------------------------------------\n");

        // 4. Totales y Pie de página
        if (!esTicketCambio) {
            ticket.append(String.format("TOTAL A PAGAR: $%.2f\n", venta.getTotal()));
        } else {
            ticket.append("   VÁLIDO SOLO PARA CAMBIOS   \n");
            ticket.append("    (SIN VALOR COMERCIAL)     \n");
        }

        ticket.append("========================================\n");
        ticket.append("        ¡Gracias por tu compra!         \n");
        ticket.append("========================================\n");

        return ticket.toString();
    }
}