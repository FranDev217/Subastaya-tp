package com.unaj.subastaya;

import com.unaj.subastaya.dto.SubastaListadoResponse;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.LiquidacionService;
import com.unaj.subastaya.service.SubastaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
@Transactional
class SubastaServiceTest {

    private static final long SUBASTA_BICICLETA = 4L;
    private static final long SUBASTA_TECLADO = 5L;

    @Autowired
    private SubastaService subastaService;

    @Autowired
    private LiquidacionService liquidacionService;

    @Test
    void listadoSinFiltroDeEstadoMuestraFinalizadasYDesiertasAlFinal() {
        // Vencidas por fecha pero todavía ACTIVA hasta que el Worker (o este cierre manual) las liquide.
        liquidacionService.cerrarSubasta(SUBASTA_BICICLETA); // tiene puja ganadora -> FINALIZADA
        liquidacionService.cerrarSubasta(SUBASTA_TECLADO);   // sin pujas -> DESIERTA

        List<SubastaListadoResponse> listado = subastaService.buscarSubastas(null, null, null, null, "menorTiempo");

        int primerIndiceTerminado = indiceDePrimerTerminado(listado);
        assertThat(primerIndiceTerminado).isGreaterThan(0);

        for (int i = 0; i < primerIndiceTerminado; i++) {
            assertThat(yaTermino(listado.get(i))).isFalse();
        }
        for (int i = primerIndiceTerminado; i < listado.size(); i++) {
            assertThat(yaTermino(listado.get(i))).isTrue();
        }
    }

    @Test
    void ordenPorMayorPujaTambienDejaLasTerminadasAlFinal() {
        liquidacionService.cerrarSubasta(SUBASTA_BICICLETA);
        liquidacionService.cerrarSubasta(SUBASTA_TECLADO);

        List<SubastaListadoResponse> listado = subastaService.buscarSubastas(null, null, null, null, "mayorPuja");

        int primerIndiceTerminado = indiceDePrimerTerminado(listado);
        for (int i = primerIndiceTerminado; i < listado.size(); i++) {
            assertThat(yaTermino(listado.get(i))).isTrue();
        }
    }

    private int indiceDePrimerTerminado(List<SubastaListadoResponse> listado) {
        for (int i = 0; i < listado.size(); i++) {
            if (yaTermino(listado.get(i))) return i;
        }
        return listado.size();
    }

    private boolean yaTermino(SubastaListadoResponse subasta) {
        return subasta.estado() == EstadoSubasta.FINALIZADA || subasta.estado() == EstadoSubasta.DESIERTA;
    }
}
