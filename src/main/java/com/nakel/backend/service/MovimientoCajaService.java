package com.nakel.backend.service;

import com.nakel.backend.model.CajaDiaria;
import com.nakel.backend.model.MovimientoCaja;
import com.nakel.backend.model.TipoMovimientoCaja; // 🔥 Importamos tu Enum
import com.nakel.backend.model.Usuario;
import com.nakel.backend.repository.MovimientoCajaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoCajaService {

    @Autowired
    private MovimientoCajaRepository movimientoRepository;

    // 🟢 1. Traer todos los movimientos de una caja (Para el Modal del Ojito)
    public List<MovimientoCaja> obtenerMovimientosPorCaja(Long cajaId) {
        return movimientoRepository.findByCajaIdOrderByFechaHoraAsc(cajaId);
    }

    // 🟢 2. Crear y guardar un movimiento nuevo (🔥 AHORA RECIBE EL ENUM)
    public MovimientoCaja registrarMovimiento(CajaDiaria caja, Usuario usuario, TipoMovimientoCaja tipo, String concepto, String descripcion, String medioPago, BigDecimal monto) {
        MovimientoCaja mov = new MovimientoCaja();
        mov.setCaja(caja);
        mov.setUsuario(usuario);
        mov.setFechaHora(LocalDateTime.now());
        mov.setTipo(tipo); // 🔥 Asigna el Enum directamente
        mov.setConcepto(concepto);
        mov.setDescripcion(descripcion);
        mov.setMedioPago(medioPago);
        mov.setMonto(monto);

        return movimientoRepository.save(mov);
    }
}