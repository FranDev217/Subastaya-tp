package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    List<Subasta> findByEstado(EstadoSubasta estado);

    List<Subasta> findByEstadoAndFechaFinBefore(EstadoSubasta estado, LocalDateTime fecha);
}
