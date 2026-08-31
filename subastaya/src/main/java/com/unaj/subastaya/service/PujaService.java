package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.PujaRequest;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.exception.MontoInvalidoException;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.exception.SaldoInsuficienteException;
import com.unaj.subastaya.exception.SubastaNoActivaException;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.model.Usuario;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PujaService {

    private static final int VENTANA_ANTI_SNIPING_SEGUNDOS = 60;
    private static final int EXTENSION_ANTI_SNIPING_MINUTOS = 2;

    private final SubastaRepository subastaRepository;
    private final PujaRepository pujaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BilleteraService billeteraService;
    private final AuditoriaLogService auditoriaLogService;

    @Transactional
    public PujaResponse registrarPuja(Long subastaId, PujaRequest request) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subasta " + subastaId + " no encontrada"));

        if (subasta.getEstado() != EstadoSubasta.ACTIVA) {
            auditoriaLogService.registrarRechazo(TipoEntidadAuditoria.SUBASTA, subastaId, "PUJA_RECHAZADA",
                    request.compradorId(), "Subasta no activa (estado=" + subasta.getEstado() + ")");
            throw new SubastaNoActivaException(subastaId);
        }

        Optional<Puja> pujaLiderActual = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subastaId);
        BigDecimal montoActual = pujaLiderActual.map(Puja::getMonto).orElse(subasta.getPrecioBase());
        BigDecimal montoMinimo = montoActual.add(subasta.getIncrementoMinimo());

        if (request.monto().compareTo(montoMinimo) < 0) {
            auditoriaLogService.registrarRechazo(TipoEntidadAuditoria.SUBASTA, subastaId, "PUJA_RECHAZADA",
                    request.compradorId(), "Monto " + request.monto() + " menor al mínimo requerido " + montoMinimo);
            throw new MontoInvalidoException(request.monto(), montoMinimo);
        }

        Usuario comprador = usuarioRepository.findById(request.compradorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario " + request.compradorId() + " no encontrado"));

        try {
            billeteraService.congelarSaldo(comprador.getId(), request.monto(), subasta);
        } catch (SaldoInsuficienteException ex) {
            auditoriaLogService.registrarRechazo(TipoEntidadAuditoria.SUBASTA, subastaId, "PUJA_RECHAZADA",
                    request.compradorId(), ex.getMessage());
            throw ex;
        } catch (ObjectOptimisticLockingFailureException ex) {
            auditoriaLogService.registrarRechazo(TipoEntidadAuditoria.SUBASTA, subastaId, "PUJA_RECHAZADA",
                    request.compradorId(), "Conflicto de concurrencia al congelar el saldo del comprador");
            throw ex;
        }

        pujaLiderActual.ifPresent(pujaAnterior -> billeteraService.liberarSaldo(
                pujaAnterior.getComprador().getId(), pujaAnterior.getMonto(), subasta));

        Puja puja = pujaRepository.save(Puja.builder()
                .subasta(subasta)
                .comprador(comprador)
                .monto(request.monto())
                .build());

        boolean extendida = aplicarAntiSnipingSiCorresponde(subasta);

        return new PujaResponse(
                puja.getId(),
                subasta.getId(),
                comprador.getId(),
                comprador.getNombre(),
                puja.getMonto(),
                puja.getFechaPuja(),
                subasta.getFechaFin(),
                extendida
        );
    }

    private boolean aplicarAntiSnipingSiCorresponde(Subasta subasta) {
        long segundosRestantes = Duration.between(LocalDateTime.now(), subasta.getFechaFin()).getSeconds();
        if (segundosRestantes > VENTANA_ANTI_SNIPING_SEGUNDOS) {
            return false;
        }
        LocalDateTime fechaFinAnterior = subasta.getFechaFin();
        subasta.setFechaFin(fechaFinAnterior.plusMinutes(EXTENSION_ANTI_SNIPING_MINUTOS));
        auditoriaLogService.registrar(TipoEntidadAuditoria.SUBASTA, subasta.getId(), "EXTENSION_TIEMPO", null,
                "Extendida de " + fechaFinAnterior + " a " + subasta.getFechaFin() + " por anti-sniping");
        return true;
    }
}
