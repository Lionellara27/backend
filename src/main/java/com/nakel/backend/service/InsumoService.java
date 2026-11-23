package com.nakel.backend.service;

import com.nakel.backend.model.CategoriaInsumo;
import com.nakel.backend.model.Insumo;
import com.nakel.backend.model.Material;
import com.nakel.backend.repository.CategoriaInsumoRepository;
import com.nakel.backend.repository.InsumoRepository;
import com.nakel.backend.repository.MaterialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service // Le avisa a Spring que esta clase es la que piensa
public class InsumoService {

    // Traemos las dos herramientas que necesitamos para guardar
    private final InsumoRepository insumoRepository;
    private final CategoriaInsumoRepository categoriaRepository;
    private final MaterialRepository materialRepository;

    // Inyección de dependencias por constructor (Buena práctica)
    public InsumoService(InsumoRepository insumoRepository,
                         CategoriaInsumoRepository categoriaRepository,
                         MaterialRepository materialRepository) {

        this.insumoRepository = insumoRepository;
        this.categoriaRepository = categoriaRepository;
        this.materialRepository = materialRepository;
    }

    // 🟢 OBTENER TODOS (BLINDADO CON PAGINACIÓN)
    @Transactional(readOnly = true)
    public Page<Insumo> obtenerTodos(Pageable pageable) {
        return insumoRepository.findAll(pageable);
    }

    // 🟢 FILTROS DE LA PANTALLA "GESTIÓN DE INSUMOS": cualquiera puede venir vacío
    @Transactional(readOnly = true)
    public Page<Insumo> filtrar(String texto, String tipoMedicion, Long idMaterial, Pageable pageable) {
        boolean sinTexto = texto == null || texto.isBlank();
        boolean sinTipo = tipoMedicion == null || tipoMedicion.isBlank();
        boolean sinMaterial = idMaterial == null;
        return insumoRepository.filtrar(
                sinTexto, sinTexto ? "" : texto.trim(),
                sinTipo, sinTipo ? "" : tipoMedicion.trim().toUpperCase(),
                sinMaterial, sinMaterial ? -1L : idMaterial,
                pageable);
    }

    // 🟢 LISTA PARA LA CALCULADORA: horas primero, después unidades, después superficie
    @Transactional(readOnly = true)
    public Page<Insumo> listarParaCalculadora(String texto, int pagina, int tamanio) {
        boolean sinTexto = texto == null || texto.isBlank();
        // Sin orden en el Pageable: el orden lo fija la consulta
        return insumoRepository.listarParaCalculadora(sinTexto, sinTexto ? "" : texto.trim(),
                org.springframework.data.domain.PageRequest.of(Math.max(0, pagina), Math.max(1, tamanio)));
    }

    // 🟢 BUSCADOR PREDICTIVO
    @Transactional(readOnly = true)
    public Page<Insumo> buscarPorNombre(String nombre, Pageable pageable) {
        return insumoRepository.findByNombreContainingIgnoreCase(nombre, pageable);
    }

    // 🟢 GUARDAR NUEVO
    @Transactional
    public Insumo guardarInsumo(Insumo insumo) {

        // ==========================================
        // VALIDAR Y CARGAR LA CATEGORÍA
        // ==========================================
        if (insumo.getCategoria() != null && insumo.getCategoria().getId() != null) {

            CategoriaInsumo categoriaReal = categoriaRepository
                    .findById(insumo.getCategoria().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Error: La categoría seleccionada no existe en el sistema."));

            insumo.setCategoria(categoriaReal);

        } else {
            throw new RuntimeException("Error: El insumo debe tener una categoría válida asignada.");
        }

        // ==========================================
        // VALIDAR Y CARGAR EL MATERIAL (OPCIONAL)
        // ==========================================
        if (insumo.getMaterial() != null && insumo.getMaterial().getId() != null) {

            Material materialReal = materialRepository
                    .findById(insumo.getMaterial().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Error: El material seleccionado no existe en el sistema."));

            insumo.setMaterial(materialReal);
        }

        // ==========================================
        // GUARDAR
        // ==========================================
        return insumoRepository.save(insumo);
    }

    // 🟢 ACTUALIZAR EXISTENTE
    @Transactional
    public Insumo actualizarInsumo(Long id, Insumo insumoActualizado) {
        // 1. Buscamos el viejo
        Insumo insumoExistente = insumoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Insumo no encontrado con el ID " + id));

        // 2. Le pisamos los datos con los NUEVOS nombres del modelo
        insumoExistente.setNombre(insumoActualizado.getNombre());
        insumoExistente.setCostoTotal(insumoActualizado.getCostoTotal());

        // Mapeo de los campos nuevos de LOTE
        insumoExistente.setAnchoLoteCm(insumoActualizado.getAnchoLoteCm());
        insumoExistente.setLargoLoteCm(insumoActualizado.getLargoLoteCm());
        insumoExistente.setCantidadLote(insumoActualizado.getCantidadLote());

        // 3. Verificamos la categoría
        if (insumoActualizado.getCategoria() != null && insumoActualizado.getCategoria().getId() != null) {
            CategoriaInsumo categoriaReal = categoriaRepository.findById(insumoActualizado.getCategoria().getId())
                    .orElseThrow(() -> new RuntimeException("Error: La nueva categoría no existe."));
            insumoExistente.setCategoria(categoriaReal);
        }

        // 4. Guardamos
        return insumoRepository.save(insumoExistente);
    }

    // 🟢 ELIMINAR
    @Transactional
    public void eliminarInsumo(Long id) {
        if (!insumoRepository.existsById(id)) {
            throw new RuntimeException("Error: No se puede eliminar. El insumo no existe.");
        }
        insumoRepository.deleteById(id);
    }
}