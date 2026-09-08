package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.AuditoriaLogResponse;
import com.unaj.subastaya.model.AuditoriaLog;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.repository.AuditoriaLogRepository;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<AuditoriaLogResponse> listar(TipoEntidadAuditoria entidad, Long entidadId) {
        return auditoriaLogRepository.findByEntidadAndEntidadIdOrderByFechaDesc(entidad, entidadId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AuditoriaLogResponse toResponse(AuditoriaLog log) {
        return new AuditoriaLogResponse(
                log.getId(),
                log.getEntidad(),
                log.getEntidadId(),
                log.getAccion(),
                log.getUsuario() == null ? null : log.getUsuario().getId(),
                log.getDetalleJson(),
                log.getFecha()
        );
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
