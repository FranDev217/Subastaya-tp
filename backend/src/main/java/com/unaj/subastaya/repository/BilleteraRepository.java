package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.Billetera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BilleteraRepository extends JpaRepository<Billetera, Long> {

    Optional<Billetera> findByUsuarioId(Long usuarioId);
}
