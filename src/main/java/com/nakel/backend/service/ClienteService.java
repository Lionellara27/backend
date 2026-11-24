package com.nakel.backend.service;

import com.nakel.backend.model.Cliente;
import com.nakel.backend.repository.ClienteRepository;
import com.nakel.backend.repository.VentaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ClienteService {

    private static final String CUIT_CONSUMIDOR_FINAL = "00000000";

    private final ClienteRepository repository;
    private final VentaRepository ventaRepository;
    private final ValeService valeService;

    public ClienteService(ClienteRepository repository, VentaRepository ventaRepository, ValeService valeService) {
        this.repository = repository;
        this.ventaRepository = ventaRepository;
        this.valeService = valeService;
    }

    /**
     * El SALDO A FAVOR de un cliente no se escribe a mano: es la suma de sus vales vigentes.
     * Sube cuando se le emite un vale, baja cuando lo usa y vuelve a 0 cuando el vale vence.
     * Acá se calcula en el momento, cada vez que se muestra un cliente.
     */
    private Cliente conSaldoReal(Cliente cliente) {
        if (cliente != null && cliente.getId() != null) {
            cliente.setSaldoAFavor(valeService.saldoAFavorDe(cliente.getId()));
        }
        return cliente;
    }

    // Para una página entera se piden todos los saldos en UNA consulta (antes era una por cliente)
    private Page<Cliente> conSaldoReal(Page<Cliente> pagina) {
        java.util.List<Long> ids = pagina.getContent().stream().map(Cliente::getId).filter(java.util.Objects::nonNull).toList();
        java.util.Map<Long, java.math.BigDecimal> saldos = valeService.saldosAFavorDe(ids);
        pagina.forEach(c -> c.setSaldoAFavor(saldos.getOrDefault(c.getId(), java.math.BigDecimal.ZERO)));
        return pagina;
    }

    // 🔥 LA MAGIA: Este método se ejecuta solo apenas arranca el sistema
    @PostConstruct
    public void inicializarConsumidorFinal() {
        // 1. Buscamos si ya existe el cliente con CUIT 00000000
        if (repository.findByCuit(CUIT_CONSUMIDOR_FINAL).isEmpty()) {

            // 2. Si no existe (porque es una PC nueva), lo creamos
            Cliente consumidorFinal = new Cliente();
            consumidorFinal.setNombre("Consumidor Final");
            consumidorFinal.setCuit(CUIT_CONSUMIDOR_FINAL);
            consumidorFinal.setTelefono("");
            consumidorFinal.setEmail("");
            consumidorFinal.setCondicionIva("CONSUMIDOR_FINAL");

            // 3. Lo guardamos en la base de datos de la clienta
            repository.save(consumidorFinal);
            System.out.println("🛡️ Cliente 'Consumidor Final' creado automáticamente por defecto.");
        }
    }

    // 1. 🔥 Listado Paginado: Adiós a traer miles de registros de golpe
    @Transactional(readOnly = true)
    public Page<Cliente> obtenerTodos(Pageable pageable) {
        return conSaldoReal(repository.findAll(pageable));
    }

    @Transactional(readOnly = true)
    public Page<Cliente> buscarGlobal(String texto, Pageable pageable) {

        if (texto == null || texto.isBlank()) {
            return conSaldoReal(repository.findAll(pageable));
        }

        String busqueda = texto.trim();

        // Si parece un CUIT/DNI, buscamos por CUIT
        if (busqueda.matches("\\d+")) {
            return conSaldoReal(repository.findByCuitContainingIgnoreCase(
                    busqueda,
                    pageable
            ));
        }

        // Si no, buscamos por nombre
        return conSaldoReal(repository.findByNombreContainingIgnoreCase(
                busqueda,
                pageable
        ));
    }

    // 3. Validación de CUIT
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        if (cliente.getNombre() == null || cliente.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del cliente es obligatorio.");
        }

        // El CUIT/DNI es único en la base. Un texto vacío "" cuenta como valor repetido,
        // así que un cliente sin documento se guarda con NULL (se permiten muchos NULL).
        if (cliente.getCuit() == null || cliente.getCuit().isBlank()) {
            cliente.setCuit(null);
        } else {
            cliente.setCuit(cliente.getCuit().trim());
            Optional<Cliente> existente = repository.findByCuit(cliente.getCuit());
            if (existente.isPresent() && !existente.get().getId().equals(cliente.getId())) {
                throw new RuntimeException("Ya existe un cliente registrado con el CUIT/DNI " + cliente.getCuit());
            }
        }

        if (cliente.getCondicionIva() == null || cliente.getCondicionIva().isBlank()) {
            cliente.setCondicionIva("CONSUMIDOR_FINAL");
        }
        // El saldo a favor NUNCA se toma de lo que manda la pantalla: sale de los vales
        cliente.setSaldoAFavor(cliente.getId() == null
                ? java.math.BigDecimal.ZERO
                : valeService.saldoAFavorDe(cliente.getId()));
        if (cliente.getSaldoPendiente() == null) {
            cliente.setSaldoPendiente(java.math.BigDecimal.ZERO);
        }
        return repository.save(cliente);
    }

    @Transactional(readOnly = true)
    public Optional<Cliente> buscarPorCuit(String cuit) {
        return repository.findByCuit(cuit).map(this::conSaldoReal);
    }

    // 4. Actualización
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente clienteActualizado) {
        Cliente existente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró el cliente."));

        existente.setNombre(clienteActualizado.getNombre());
        existente.setCuit(clienteActualizado.getCuit());
        existente.setCondicionIva(clienteActualizado.getCondicionIva());
        existente.setTelefono(clienteActualizado.getTelefono());
        existente.setEmail(clienteActualizado.getEmail());

        // El saldo a favor no se edita a mano (lo calcula guardarCliente desde los vales).
        // La deuda pendiente sí se puede corregir.
        if (clienteActualizado.getSaldoPendiente() != null) {
            existente.setSaldoPendiente(clienteActualizado.getSaldoPendiente());
        }

        return guardarCliente(existente);
    }

    // 5. Eliminación
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("El cliente ya no existe."));

        if (CUIT_CONSUMIDOR_FINAL.equals(cliente.getCuit())) {
            throw new RuntimeException("El cliente 'Consumidor Final' es del sistema y no se puede eliminar.");
        }

        // SQLite no hace cumplir las claves foráneas, así que lo controlamos a mano:
        // un cliente con ventas no se borra, para no perder ni romper el historial.
        long ventas = ventaRepository.countByClienteId(id);
        if (ventas > 0) {
            throw new RuntimeException("No se puede eliminar: el cliente tiene " + ventas
                    + (ventas == 1 ? " venta registrada." : " ventas registradas."));
        }

        repository.delete(cliente);
    }
}
