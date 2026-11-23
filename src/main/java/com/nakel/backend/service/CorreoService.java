package com.nakel.backend.service;

import com.nakel.backend.model.DetalleVenta;
import com.nakel.backend.model.Venta;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoService {

    private final JavaMailSender mailSender;

    // Cuenta de Gmail configurada en application.properties
    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String cuentaRemitente;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarTicketPorEmail(String emailDestino, Venta venta) {
        // Mientras sigan los datos de ejemplo no hay forma de enviar: avisamos claro en vez de fallar raro
        if (cuentaRemitente == null || cuentaRemitente.isBlank() || cuentaRemitente.startsWith("tu_correo")) {
            throw new IllegalStateException("El envío por mail todavía no está configurado en este sistema.");
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(cuentaRemitente);
        mensaje.setTo(emailDestino);
        mensaje.setSubject("Tu comprobante de compra - Nakel"); // Nombre del local

        // Armamos el texto del ticket igual que en la impresora
        StringBuilder sb = new StringBuilder();
        // fechaHora es LocalDateTime: SimpleDateFormat no lo sabe formatear y tiraba excepción siempre
        java.time.format.DateTimeFormatter formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        sb.append("¡Hola! Gracias por tu compra.\n\n");
        sb.append("================================\n");
        sb.append("         TICKET DE VENTA        \n");
        sb.append("================================\n");
        sb.append("Fecha: ").append(venta.getFechaHora() != null ? venta.getFechaHora().format(formato) : "-").append("\n");
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