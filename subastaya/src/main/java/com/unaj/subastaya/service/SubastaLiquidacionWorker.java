package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.model.EstadoSubasta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Cierra las subastas que ya vencieron: las liquida si tienen pujas o las marca
 * DESIERTA si no recibió ninguna, y difunde el cierre por WebSocket.
 *
 * <p>El bean se puede desactivar con {@code subastaya.worker.enabled=false}, que
 * es lo que hacen los tests para que la tarea no cierre las subastas del seed
 * mientras corren.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "subastaya.worker.enabled", matchIfMissing = true)
public class SubastaLiquidacionWorker {

    private static final long INTERVALO_ENTRE_CORRIDAS_MS = 60_000;

    private final LiquidacionService liquidacionService;
    private final SubastaService subastaService;
    private final SubastaNotificador subastaNotificador;

    @Scheduled(fixedDelay = INTERVALO_ENTRE_CORRIDAS_MS)
    public void cerrarSubastasVencidas() {
        liquidacionService.obtenerSubastasVencidas()
                .forEach(subasta -> cerrar(subasta.getId()));
    }

    /**
     * Cada subasta se cierra de a una para aislar los fallos: si una no se puede
     * adjudicar (por un conflicto de concurrencia o por inconsistencia de saldos),
     * se registra el error y se pasa a la siguiente en lugar de abortar la corrida.
     */
    private void cerrar(Long subastaId) {
        try {
            liquidacionService.cerrarSubasta(subastaId)
                    .ifPresent(estado -> notificarCierre(subastaId, estado));
        } catch (RuntimeException ex) {
            log.warn("No se pudo cerrar la subasta {}: se reprocesará en la próxima corrida", subastaId, ex);
        }
    }

    private void notificarCierre(Long subastaId, EstadoSubasta estadoCierre) {
        SubastaEvento estadoFinal = subastaService.estadoActual(subastaId);
        subastaNotificador.notificar(subastaId, new SubastaEvento(
                tipoEventoDe(estadoCierre),
                estadoFinal.subastaId(),
                estadoFinal.estado(),
                estadoFinal.montoActual(),
                estadoFinal.fechaFin(),
                estadoFinal.ultimaPuja()
        ));
    }

    private TipoEvento tipoEventoDe(EstadoSubasta estadoCierre) {
        return estadoCierre == EstadoSubasta.DESIERTA ? TipoEvento.DESIERTA : TipoEvento.FINALIZADA;
    }
}
