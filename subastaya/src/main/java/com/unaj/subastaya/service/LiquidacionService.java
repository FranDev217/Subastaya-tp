package com.unaj.subastaya.service;

import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LiquidacionService {

    private final SubastaRepository subastaRepository;
    private final PujaRepository pujaRepository;
    private final BilleteraService billeteraService;

    /**
     * Subastas que ya pasaron su fecha de cierre y todavía figuran como ACTIVA,
     * es decir, las que el Worker todavía no adjudicó.
     */
    @Transactional(readOnly = true)
    public List<Subasta> obtenerSubastasVencidas() {
        return subastaRepository.findByEstadoAndFechaFinBefore(EstadoSubasta.ACTIVA, LocalDateTime.now());
    }

    /**
     * Adjudica una subasta ya vencida: la marca DESIERTA si no recibió pujas.
     *
     * <p>Se revalida el estado y la fecha de cierre dentro de la transacción porque
     * entre la consulta del Worker y este momento puede haber entrado una puja
     * (y con ella una extensión por anti-sniping) o puede haber cerrado la subasta
     * otra instancia del proceso. En ese caso no se hace nada y la subasta se
     * reprocesa en la siguiente corrida.</p>
     */
    @Transactional
    public void cerrarSubasta(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subasta " + subastaId + " no encontrada"));

        if (subasta.getEstado() != EstadoSubasta.ACTIVA || !estaVencida(subasta)) {
            return;
        }

        Optional<Puja> pujaGanadora = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subastaId);

        if (pujaGanadora.isEmpty()) {
            marcarDesierta(subasta);
            return;
        }

        liquidar(subasta, pujaGanadora.get());
    }

    private void marcarDesierta(Subasta subasta) {
        subasta.setEstado(EstadoSubasta.DESIERTA);
    }

    /**
     * Liquidación final: debita al comprador ganador y acredita al vendedor en la
     * misma transacción en la que la subasta pasa a FINALIZADA, de modo que el
     * cambio de estado, los dos movimientos de billetera y los asientos del Ledger
     * se confirmen o se deshagan juntos.
     */
    private void liquidar(Subasta subasta, Puja pujaGanadora) {
        BigDecimal montoVenta = pujaGanadora.getMonto();

        // primero el débito: si el comprador no tiene el saldo retenido, falla antes de acreditar al vendedor
        billeteraService.pagar(pujaGanadora.getComprador().getId(), montoVenta, subasta);
        billeteraService.cobrar(subasta.getVendedor().getId(), montoVenta, subasta);

        subasta.setEstado(EstadoSubasta.FINALIZADA);
    }

    private boolean estaVencida(Subasta subasta) {
        return subasta.getFechaFin().isBefore(LocalDateTime.now());
    }
}
