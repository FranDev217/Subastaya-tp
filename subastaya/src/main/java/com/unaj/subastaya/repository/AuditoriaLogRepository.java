package com.unaj.subastaya.repository;

import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    List<AuditoriaLog> findByEntidadAndEntidadId(TipoEntidadAuditoria entidad, Long entidadId);
}
