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

    @Autowired
    private ArticuloService articuloService;

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
        nuevoArticulo.setCosto(dto.getCosto()); // costo de producción de UNA unidad, según la receta
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

        // Si el código lo tiene un artículo dado de baja lo liberamos; si lo tiene uno activo, error claro
        articuloService.liberarCodigo(nuevoArticulo.getCodigo(), null);

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

        // 2. Buscamos el último artículo de la categoría, CONTANDO los dados de baja:
        //    su código sigue ocupado en la base, así que no se puede volver a entregar.
        java.util.List<Articulo> ultimos = articuloRepository.findUltimoPorCategoriaIncluyendoBajas(idCategoria);

        String partePrefijo = prefijoReal + "-";

        // 3. Del artículo más nuevo cuyo código termine en 4 números tomamos solo el PREFIJO
        //    (por si los códigos de la categoría se escribieron distinto al prefijo configurado).
        //    Los renombrados "-BAJA-" se saltean solos.
        if (ultimos != null) {
            for (Articulo art : ultimos) {
                String sku = art.getCodigo();
                if (sku == null || sku.length() < 4 || sku.contains("-BAJA-")) continue;
                String ultimosCuatro = sku.substring(sku.length() - 4);
                if (ultimosCuatro.matches("\\d{4}")) {
                    partePrefijo = sku.substring(0, sku.length() - 4);
                    break;
                }
            }
        }

        // 4. Se sugiere el PRIMER número libre empezando en 0001, así se rellenan los huecos:
        //    con 0001 y 0010-0014 cargados sugiere 0002; con todo lleno hasta 0014, 0015.
        //    (Antes seguía desde el último cargado y los huecos no se usaban nunca.)
        //    Un código cuenta como ocupado aunque el artículo esté dado de baja.
        int siguienteNum = 1;
        String candidato = partePrefijo + String.format("%04d", siguienteNum);
        int intentos = 0;
        while (articuloRepository.findFirstByCodigo(candidato).isPresent() && intentos < 10000) {
            siguienteNum++;
            intentos++;
            candidato = partePrefijo + String.format("%04d", siguienteNum);
        }
        return candidato;
    }
}