package com.nakel.backend.service;

import com.nakel.backend.model.DetalleVenta;
import com.nakel.backend.model.Venta;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.text.SimpleDateFormat;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarTicketPorEmail(String emailDestino, Venta venta) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(emailDestino);
        mensaje.setSubject("Tu comprobante de compra - Nakel"); // Nombre del local

        // Armamos el texto del ticket igual que en la impresora
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        sb.append("¡Hola! Gracias por tu compra.\n\n");
        sb.append("================================\n");
        sb.append("         TICKET DE VENTA        \n");
        sb.append("================================\n");
        sb.append("Fecha: ").append(sdf.format(venta.getFechaHora())).append("\n");
        sb.append("--------------------------------\n");

        for (DetalleVenta det : venta.getDetalles()) {
            sb.append(det.getCantidad()).append("x ").append(det.getArticulo().getNombre())
                    .append(" - $").append(det.getSubtotal()).append("\n");
        }

        sb.append("--------------------------------\n");
        sb.append("TOTAL: $").append(venta.getTotal()).append("\n");
        sb.append("================================\n");

        mensaje.setText(sb.toString());

        // Disparamos el email
        mailSender.send(mensaje);
        System.out.println("✅ Email enviado exitosamente a: " + emailDestino);
    }
}