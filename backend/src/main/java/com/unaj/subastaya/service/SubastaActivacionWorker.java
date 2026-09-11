package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.model.EstadoSubasta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Abre las subastas PROGRAMADA cuyo {@code fechaInicio} ya llegó y difunde la
 * apertura por WebSocket. Sigue el mismo patrón que {@link SubastaLiquidacionWorker}:
 * el initialDelay es configurable para que los tests lo invoquen a mano.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubastaActivacionWorker {

    private static final long INTERVALO_ENTRE_CORRIDAS_MS = 60_000;

    private final SubastaService subastaService;
    private final SubastaNotificador subastaNotificador;

    @Scheduled(fixedDelay = INTERVALO_ENTRE_CORRIDAS_MS,
            initialDelayString = "${subastaya.worker.initial-delay-ms:0}")
    public void activarSubastasProgramadas() {
        subastaService.obtenerSubastasProgramadasParaActivar()
                .forEach(subasta -> activar(subasta.getId()));
    }

    private void activar(Long subastaId) {
        try {
            subastaService.activarSubasta(subastaId)
                    .ifPresent(estado -> notificarApertura(subastaId));
        } catch (RuntimeException ex) {
            log.warn("No se pudo activar la subasta {}: se reprocesará en la próxima corrida", subastaId, ex);
        }
    }

    private void notificarApertura(Long subastaId) {
        SubastaEvento estadoActual = subastaService.estadoActual(subastaId);
        subastaNotificador.notificar(subastaId, new SubastaEvento(
                TipoEvento.ESTADO_CAMBIADO,
                estadoActual.subastaId(),
                EstadoSubasta.ACTIVA,
                estadoActual.montoActual(),
                estadoActual.fechaFin(),
                estadoActual.ultimaPuja()
        ));
    }
}
