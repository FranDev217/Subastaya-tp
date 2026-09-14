package com.unaj.subastaya;

import com.unaj.subastaya.dto.MiCompraResponse;
import com.unaj.subastaya.dto.MiPublicacionResponse;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.ActividadService;
import com.unaj.subastaya.service.LiquidacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "subastaya.worker.initial-delay-ms=600000")
@Sql(scripts = "/sql/reset-seed.sql")
@Transactional
class ActividadServiceTest {

    private static final long USUARIO_VENDEDOR = 1L;
    private static final long USUARIO_COMPRADOR_1 = 2L;
    private static final long USUARIO_COMPRADOR_2 = 3L;
    private static final long SUBASTA_BICICLETA = 4L;

    @Autowired
    private ActividadService actividadService;

    @Autowired
    private LiquidacionService liquidacionService;

    @Test
    void misPublicacionesListaTodasLasSubastasDelVendedor() {
        List<MiPublicacionResponse> publicaciones = actividadService.misPublicaciones(USUARIO_VENDEDOR);

        assertThat(publicaciones).extracting(MiPublicacionResponse::id)
                .contains(1L, 2L, 3L, SUBASTA_BICICLETA, 5L);
    }

    @Test
    void misPublicacionesSoloMuestraRecaudacionCuandoLaSubastaSeFinaliza() {
        MiPublicacionResponse antes = buscarPublicacion(USUARIO_VENDEDOR, SUBASTA_BICICLETA);
        assertThat(antes.estado()).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(antes.ofertaActual()).isEqualByComparingTo("32000");
        assertThat(antes.recaudacion()).isNull();

        liquidacionService.cerrarSubasta(SUBASTA_BICICLETA);

        MiPublicacionResponse despues = buscarPublicacion(USUARIO_VENDEDOR, SUBASTA_BICICLETA);
        assertThat(despues.estado()).isEqualTo(EstadoSubasta.FINALIZADA);
        assertThat(despues.recaudacion()).isEqualByComparingTo("32000");
    }

    @Test
    void misComprasIndicaSiElUsuarioLideraYSiGano() {
        MiCompraResponse antes = buscarCompra(USUARIO_COMPRADOR_2, SUBASTA_BICICLETA);
        assertThat(antes.estado()).isEqualTo(EstadoSubasta.ACTIVA);
        assertThat(antes.soyElLider()).isTrue();
        assertThat(antes.miMejorPuja()).isEqualByComparingTo("32000");

        liquidacionService.cerrarSubasta(SUBASTA_BICICLETA);

        MiCompraResponse despues = buscarCompra(USUARIO_COMPRADOR_2, SUBASTA_BICICLETA);
        assertThat(despues.estado()).isEqualTo(EstadoSubasta.FINALIZADA);
        assertThat(despues.soyElLider()).isTrue();
    }

    @Test
    void misComprasNoIncluyeSubastasSinPujasDelUsuario() {
        List<MiCompraResponse> compras = actividadService.misCompras(USUARIO_COMPRADOR_1);

        assertThat(compras).extracting(MiCompraResponse::subastaId).doesNotContain(SUBASTA_BICICLETA);
    }

    @Test
    void misPublicacionesFallaSiElUsuarioNoExiste() {
        assertThatThrownBy(() -> actividadService.misPublicaciones(9999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    private MiPublicacionResponse buscarPublicacion(long usuarioId, long subastaId) {
        return actividadService.misPublicaciones(usuarioId).stream()
                .filter(p -> p.id() == subastaId)
                .findFirst()
                .orElseThrow();
    }

    private MiCompraResponse buscarCompra(long usuarioId, long subastaId) {
        return actividadService.misCompras(usuarioId).stream()
                .filter(c -> c.subastaId() == subastaId)
                .findFirst()
                .orElseThrow();
    }
}
