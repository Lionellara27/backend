package com.nakel.backend.repository;

import com.nakel.backend.model.Insumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    // 🔥 BLINDAJE 1: @EntityGraph mata el N+1 (1 sola consulta SQL)
    // 🔥 BLINDAJE 2: Page<T> mata el consumo de RAM (trae de a ej: 20 registros)
    @Override
    @EntityGraph(attributePaths = {"categoria"})
    Page<Insumo> findAll(Pageable pageable);

    // 🔥 El buscador predictivo también blindado contra la RAM
    @EntityGraph(attributePaths = {"categoria"})
    Page<Insumo> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);

    // Filtros de "Gestión de Insumos" (texto, cómo se mide, material) aplicados sobre TODOS los insumos.
    // Antes la pantalla filtraba solo los 20 de la página que tenía cargada: si el insumo estaba
    // en otra página (ej: la mano de obra, que es el último) el filtro no encontraba nada.
    // Los "sin..." indican que ese filtro no se usa (mismo patrón que el historial de ventas).
    @EntityGraph(attributePaths = {"categoria"})
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT i FROM Insumo i LEFT JOIN i.categoria c LEFT JOIN i.material m " +
                    "WHERE (:sinTexto = true OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
                    "AND (:sinTipo = true OR c.tipoMedicion = :tipo) " +
                    "AND (:sinMaterial = true OR m.id = :idMaterial)",
            countQuery = "SELECT COUNT(i) FROM Insumo i LEFT JOIN i.categoria c LEFT JOIN i.material m " +
                    "WHERE (:sinTexto = true OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
                    "AND (:sinTipo = true OR c.tipoMedicion = :tipo) " +
                    "AND (:sinMaterial = true OR m.id = :idMaterial)")
    Page<Insumo> filtrar(@org.springframework.data.repository.query.Param("sinTexto") boolean sinTexto,
                         @org.springframework.data.repository.query.Param("texto") String texto,
                         @org.springframework.data.repository.query.Param("sinTipo") boolean sinTipo,
                         @org.springframework.data.repository.query.Param("tipo") String tipo,
                         @org.springframework.data.repository.query.Param("sinMaterial") boolean sinMaterial,
                         @org.springframework.data.repository.query.Param("idMaterial") Long idMaterial,
                         Pageable pageable);

    // Lista de insumos para la Calculadora de Producción: con buscador, paginada y ordenada como se
    // arma una receta: primero la mano de obra (horas), después lo que va por unidad (cierres, herrajes)
    // y al final lo que se mide por superficie (telas, cueros). Dentro de cada grupo, por nombre.
    @EntityGraph(attributePaths = {"categoria"})
    @org.springframework.data.jpa.repository.Query(
            value = "SELECT i FROM Insumo i LEFT JOIN i.categoria c " +
                    "WHERE (:sinTexto = true OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))) " +
                    "ORDER BY CASE WHEN c.tipoMedicion IN ('TIEMPO', 'SERVICIO') THEN 0 " +
                    "              WHEN c.tipoMedicion = 'UNIDAD' THEN 1 ELSE 2 END, LOWER(i.nombre), i.id",
            countQuery = "SELECT COUNT(i) FROM Insumo i " +
                    "WHERE (:sinTexto = true OR LOWER(i.nombre) LIKE LOWER(CONCAT('%', :texto, '%')))")
    Page<Insumo> listarParaCalculadora(@org.springframework.data.repository.query.Param("sinTexto") boolean sinTexto,
                                       @org.springframework.data.repository.query.Param("texto") String texto,
                                       Pageable pageable);

    // Para impedir borrar materiales o categorías de insumo que estén en uso
    long countByMaterialId(Long materialId);

    long countByCategoriaId(Long categoriaId);

}