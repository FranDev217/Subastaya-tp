package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    List<Subasta> findByEstado(EstadoSubasta estado);

    List<Subasta> findByEstadoAndFechaFinBefore(EstadoSubasta estado, LocalDateTime fecha);

    @Query("SELECT s FROM Subasta s WHERE " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:categoriaId IS NULL OR s.categoria.id = :categoriaId) AND " +
           "(:precioMin IS NULL OR s.precioBase >= :precioMin) AND " +
           "(:precioMax IS NULL OR s.precioBase <= :precioMax) " +
           "ORDER BY s.fechaFin ASC")
    List<Subasta> buscarConFiltros(
            @Param("estado") EstadoSubasta estado,
            @Param("categoriaId") Long categoriaId,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax
    );
}
