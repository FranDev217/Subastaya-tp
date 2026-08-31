package com.unaj.subastaya.service;

import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaLogService {

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrar(TipoEntidadAuditoria entidad, Long entidadId, String accion, Long usuarioId,
                           String detalleJson) {
        auditoriaLogRepository.save(construir(entidad, entidadId, accion, usuarioId, detalleJson));
    }

    // REQUIRES_NEW: el registro debe sobrevivir aunque la transacción que lo dispara haga rollback (puja rechazada)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRechazo(TipoEntidadAuditoria entidad, Long entidadId, String accion, Long usuarioId,
                                  String detalleJson) {
        auditoriaLogRepository.save(construir(entidad, entidadId, accion, usuarioId, detalleJson));
    }

    private AuditoriaLog construir(TipoEntidadAuditoria entidad, Long entidadId, String accion, Long usuarioId,
                                    String detalleJson) {
        return AuditoriaLog.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .usuario(usuarioId == null ? null : usuarioRepository.getReferenceById(usuarioId))
                .detalleJson(detalleJson)
                .build();
    }
}
