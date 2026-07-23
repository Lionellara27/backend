package com.nakel.backend.service;

import com.nakel.backend.dto.ProduccionRequestDTO;
import com.nakel.backend.model.Articulo;
import com.nakel.backend.model.Categoria;
import com.nakel.backend.model.Insumo;
import com.nakel.backend.model.Material;
import com.nakel.backend.repository.ArticuloRepository;
import com.nakel.backend.repository.CategoriaRepository;
import com.nakel.backend.repository.InsumoRepository;
import com.nakel.backend.repository.MaterialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProduccionService {

    @Autowired
    private ArticuloRepository articuloRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Transactional(rollbackFor = Exception.class) // 🔥 Si algo falla, deshace todo
    public void procesarFabricacionYAlta(ProduccionRequestDTO dto) throws Exception {

        // 🛑 PASO 1: VALIDAR STOCK PRIMERO (El Patovica del Backend)
        for (ProduccionRequestDTO.ItemRecetaDTO item : dto.getInsumosUsados()) {
            Insumo insumo = insumoRepository.findById(item.getIdInsumo())
                    .orElseThrow(() -> new Exception("Insumo no encontrado en BD."));

            String tipoMedicion = insumo.getCategoria() != null ? insumo.getCategoria().getTipoMedicion() : "";

            if ("SUPERFICIE".equals(tipoMedicion)) {
                int areaNecesaria = (int) (item.getCantidadUsada().doubleValue() * dto.getStock());
                int areaActual = insumo.getAreaActualCm2() != null ? insumo.getAreaActualCm2() : 0;

                if (areaActual < areaNecesaria) {
                    throw new Exception("Stock insuficiente de: " + insumo.getNombre());
                }
            } else if ("UNIDAD".equals(tipoMedicion)) {
                int unidadesNecesarias = item.getCantidadUsada().intValue() * dto.getStock();
                int cantidadActual = insumo.getCantidadActual() != null ? insumo.getCantidadActual() : 0;

                if (cantidadActual < unidadesNecesarias) {
                    throw new Exception("Stock insuficiente de: " + insumo.getNombre());
                }
            }
        }

        // 📦 PASO 2: CREAR Y GUARDAR EL ARTÍCULO NUEVO
        Articulo nuevoArticulo = new Articulo();
        nuevoArticulo.setNombre(dto.getNombre());
        nuevoArticulo.setCodigo(dto.getCodigo()); // El SKU autogenerado
        nuevoArticulo.setPrecio(dto.getPrecioVenta()); // Precio final corregido (sin pisar con costo)
        nuevoArticulo.setStockActual(dto.getStock());
        nuevoArticulo.setOrigen(dto.getOrigen()); // "Produccion Propia"

        nuevoArticulo.setAlicuotaIva(21.0); // Dato por defecto para la Fase 2

        // Buscar y setear Categoria y Material
        if (dto.getIdCategoria() != null) {
            Categoria cat = categoriaRepository.findById(dto.getIdCategoria()).orElse(null);
            nuevoArticulo.setCategoria(cat);
        }
        if (dto.getIdMaterial() != null) {
            Material mat = materialRepository.findById(dto.getIdMaterial()).orElse(null);
            nuevoArticulo.setMaterial(mat);
        }

        // Guardamos el artículo en la BD
        articuloRepository.save(nuevoArticulo);

        // ✂️ PASO 3: DESCONTAR EL STOCK DE LOS INSUMOS REALMENTE
        for (ProduccionRequestDTO.ItemRecetaDTO item : dto.getInsumosUsados()) {
            Insumo insumo = insumoRepository.findById(item.getIdInsumo()).get();
            String tipoMedicion = insumo.getCategoria() != null ? insumo.getCategoria().getTipoMedicion() : "";

            if ("SUPERFICIE".equals(tipoMedicion)) {
                int areaRestar = (int) (item.getCantidadUsada().doubleValue() * dto.getStock());
                insumo.setAreaActualCm2(insumo.getAreaActualCm2() - areaRestar);
                insumoRepository.save(insumo);

            } else if ("UNIDAD".equals(tipoMedicion)) {
                int unidadesRestar = item.getCantidadUsada().intValue() * dto.getStock();
                insumo.setCantidadActual(insumo.getCantidadActual() - unidadesRestar);
                insumoRepository.save(insumo);
            }
        }
    }

    // 🔍 MÉTODO BLINDADO PARA BUSCAR EL SKU CORRECTO RESPETANDO PARÁMETROS
    public String obtenerSiguienteSku(Long idCategoria) {
        // 1. Buscamos la categoría real en la BD para sacar su prefijo configurado (ej: "1111" o "2222")
        Categoria categoria = categoriaRepository.findById(idCategoria).orElse(null);

        String prefijoReal = (categoria != null && categoria.getPrefijoSku() != null && !categoria.getPrefijoSku().isBlank())
                ? categoria.getPrefijoSku().trim()
                : String.format("%04d", idCategoria);

        // 2. Buscamos el último artículo guardado en esta categoría
        java.util.List<Articulo> ultimos = articuloRepository.findUltimoPorCategoria(idCategoria);

        // 3. Si no hay ningún artículo previo (categoría virgen como Mochila), arranca limpio con prefijo + "-0001"
        if (ultimos == null || ultimos.isEmpty()) {
            return prefijoReal + "-0001";
        }

        // 4. Si ya tiene artículos (como Mate), toma el último y le suma 1 al número correlativo
        String ultimoSku = ultimos.get(0).getCodigo();
        try {
            String ultimosCuatro = ultimoSku.substring(ultimoSku.length() - 4);
            int siguienteNum = Integer.parseInt(ultimosCuatro) + 1;

            // Mantiene la estructura de prefijo que ya traía el último
            String partePrefijo = ultimoSku.substring(0, ultimoSku.length() - 4);
            return partePrefijo + String.format("%04d", siguienteNum);
        } catch (Exception e) {
            return prefijoReal + "-0001";
        }
    }
}