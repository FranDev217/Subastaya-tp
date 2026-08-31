package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.TransaccionLedger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransaccionLedgerRepository extends JpaRepository<TransaccionLedger, Long> {

    List<TransaccionLedger> findByBilleteraIdOrderByFechaDesc(Long billeteraId);
}
