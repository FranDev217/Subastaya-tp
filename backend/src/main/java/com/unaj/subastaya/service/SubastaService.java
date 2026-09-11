package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.CategoriaResponse;
import com.unaj.subastaya.dto.PujaResponse;
import com.unaj.subastaya.dto.SubastaCreadaResponse;
import com.unaj.subastaya.dto.SubastaEvento;
import com.unaj.subastaya.dto.SubastaListadoResponse;
import com.unaj.subastaya.dto.SubastaRequest;
import com.unaj.subastaya.dto.TipoEvento;
import com.unaj.subastaya.exception.RecursoNoEncontradoException;
import com.unaj.subastaya.model.AccionAuditoria;
import com.unaj.subastaya.model.Categoria;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.model.Puja;
import com.unaj.subastaya.model.Subasta;
import com.unaj.subastaya.model.TipoEntidadAuditoria;
import com.unaj.subastaya.model.Usuario;
import com.unaj.subastaya.repository.CategoriaRepository;
import com.unaj.subastaya.repository.PujaRepository;
import com.unaj.subastaya.repository.SubastaRepository;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubastaService {

    private final SubastaRepository subastaRepository;
    private final PujaRepository pujaRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaLogService auditoriaLogService;

    @Transactional
    public SubastaCreadaResponse crearSubasta(SubastaRequest request) {
        Usuario vendedor = usuarioRepository.findById(request.vendedorId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario " + request.vendedorId() + " no encontrado"));

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoría " + request.categoriaId() + " no encontrada"));

        EstadoSubasta estado = request.fechaInicio().isAfter(LocalDateTime.now())
                ? EstadoSubasta.PROGRAMADA
                : EstadoSubasta.ACTIVA;

        Subasta subasta = subastaRepository.save(Subasta.builder()
                .vendedor(vendedor)
                .categoria(categoria)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .urlImagen(request.urlImagen())
                .precioBase(request.precioBase())
                .incrementoMinimo(request.incrementoMinimo())
                .fechaInicio(request.fechaInicio())
                .fechaFin(request.fechaFin())
                .estado(estado)
                .build());

        auditoriaLogService.registrar(TipoEntidadAuditoria.SUBASTA, subasta.getId(),
                AccionAuditoria.SUBASTA_CREADA, vendedor.getId(),
                "Subasta creada en estado " + estado);

        return toCreadaResponse(subasta);
    }

    private SubastaCreadaResponse toCreadaResponse(Subasta subasta) {
        return new SubastaCreadaResponse(
                subasta.getId(),
                subasta.getTitulo(),
                subasta.getDescripcion(),
                subasta.getUrlImagen(),
                subasta.getCategoria().getId(),
                subasta.getCategoria().getNombre(),
                subasta.getPrecioBase(),
                subasta.getIncrementoMinimo(),
                subasta.getFechaInicio(),
                subasta.getFechaFin(),
                subasta.getEstado(),
                subasta.getVendedor().getId()
        );
    }

    @Transactional(readOnly = true)
    public List<Subasta> obtenerSubastasProgramadasParaActivar() {
        return subastaRepository.findByEstadoAndFechaInicioLessThanEqual(
                EstadoSubasta.PROGRAMADA, LocalDateTime.now());
    }

    /**
     * Abre una subasta programada cuyo inicio ya llegó. Se revalida el estado y la
     * fecha dentro de la transacción porque entre la consulta del Worker y este
     * momento la subasta pudo haber cambiado. Si no correspondía abrirla, no se
     * hace nada y se reprocesa en la siguiente corrida.
     *
     * @return el estado al que quedó la subasta, o vacío si no correspondía abrirla.
     */
    @Transactional
    public Optional<EstadoSubasta> activarSubasta(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subasta " + subastaId + " no encontrada"));

        if (subasta.getEstado() != EstadoSubasta.PROGRAMADA
                || subasta.getFechaInicio().isAfter(LocalDateTime.now())) {
            return Optional.empty();
        }

        subasta.setEstado(EstadoSubasta.ACTIVA);

        auditoriaLogService.registrar(TipoEntidadAuditoria.SUBASTA, subasta.getId(),
                AccionAuditoria.APERTURA_WORKER, null,
                "Abierta por el Worker: inicio " + subasta.getFechaInicio());

        return Optional.of(EstadoSubasta.ACTIVA);
    }

    @Transactional(readOnly = true)
    public SubastaEvento estadoActual(Long subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Subasta " + subastaId + " no encontrada"));

        PujaResponse ultimaPuja = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subastaId)
                .map(puja -> toResponse(puja, subasta))
                .orElse(null);

        BigDecimal montoActual = ultimaPuja != null ? ultimaPuja.monto() : subasta.getPrecioBase();

        return new SubastaEvento(
                TipoEvento.ESTADO_ACTUAL,
                subasta.getId(),
                subasta.getEstado(),
                montoActual,
                subasta.getFechaFin(),
                ultimaPuja
        );
    }

    @Transactional(readOnly = true)
    public List<SubastaListadoResponse> buscarSubastas(EstadoSubasta estado, Long categoriaId,
                                                        BigDecimal precioMin, BigDecimal precioMax,
                                                        String sort) {
        List<Subasta> subastas = subastaRepository.buscarConFiltros(estado, categoriaId, precioMin, precioMax);

        List<SubastaListadoResponse> respuestas = subastas.stream()
                .map(this::toListadoResponse)
                .toList();

        if ("mayorPuja".equals(sort)) {
            respuestas = respuestas.stream()
                    .sorted(Comparator.comparing(SubastaListadoResponse::ofertaActual).reversed())
                    .toList();
        }

        return respuestas;
    }

    private SubastaListadoResponse toListadoResponse(Subasta subasta) {
        Puja ultimaPuja = pujaRepository.findTopBySubastaIdOrderByMontoDesc(subasta.getId()).orElse(null);
        BigDecimal ofertaActual = ultimaPuja != null ? ultimaPuja.getMonto() : subasta.getPrecioBase();
        long cantidadPujas = pujaRepository.countBySubastaId(subasta.getId());

        return new SubastaListadoResponse(
                subasta.getId(),
                subasta.getTitulo(),
                subasta.getDescripcion(),
                subasta.getUrlImagen(),
                subasta.getCategoria().getId(),
                subasta.getCategoria().getNombre(),
                subasta.getPrecioBase(),
                ofertaActual,
                (int) cantidadPujas,
                subasta.getFechaInicio(),
                subasta.getFechaFin(),
                subasta.getEstado()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> obtenerCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre()))
                .toList();
    }

    private PujaResponse toResponse(Puja puja, Subasta subasta) {
        return new PujaResponse(
                puja.getId(),
                subasta.getId(),
                puja.getComprador().getId(),
                puja.getComprador().getNombre(),
                puja.getMonto(),
                puja.getFechaPuja(),
                subasta.getFechaFin(),
                false
        );
    }
}
