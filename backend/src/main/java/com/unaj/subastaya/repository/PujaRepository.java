package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.Puja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PujaRepository extends JpaRepository<Puja, Long> {

    List<Puja> findBySubastaIdOrderByFechaPujaDesc(Long subastaId);

    Optional<Puja> findTopBySubastaIdOrderByMontoDesc(Long subastaId);
}
