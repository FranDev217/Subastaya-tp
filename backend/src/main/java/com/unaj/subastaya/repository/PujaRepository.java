package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PujaRepository extends JpaRepository<Puja, Long> {

    List<Puja> findBySubastaIdOrderByFechaPujaDesc(Long subastaId);

    Optional<Puja> findTopBySubastaIdOrderByMontoDesc(Long subastaId);

    Optional<Puja> findTopBySubastaIdAndCompradorIdOrderByMontoDesc(Long subastaId, Long compradorId);

    long countBySubastaId(Long subastaId);

    @Query("SELECT DISTINCT p.subasta FROM Puja p WHERE p.comprador.id = :compradorId ORDER BY p.subasta.fechaFin DESC")
    List<Subasta> buscarSubastasConPujaDe(@Param("compradorId") Long compradorId);
}
