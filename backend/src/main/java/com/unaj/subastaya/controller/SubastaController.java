package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.CategoriaResponse;
import com.unaj.subastaya.dto.ErrorResponse;
import com.unaj.subastaya.dto.SubastaCreadaResponse;
import com.unaj.subastaya.dto.SubastaDetalleResponse;
import com.unaj.subastaya.dto.SubastaListadoResponse;
import com.unaj.subastaya.dto.SubastaRequest;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.SubastaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/subastas")
@RequiredArgsConstructor
@Tag(name = "Subastas", description = "Catálogo, publicación y detalle de subastas (Módulos 1 y 2)")
public class SubastaController {

    private final SubastaService subastaService;

    @Operation(summary = "Publicar una subasta",
            description = "Crea la subasta en estado PROGRAMADA (si fechaInicio es futura) o ACTIVA "
                    + "(si ya arrancó). Registra el alta en la auditoría.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subasta creada"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (título/descr. vacíos, "
                    + "precio o incremento <= 0, fechaFin <= fechaInicio, fechaFin en el pasado)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Vendedor o categoría inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<SubastaCreadaResponse> crearSubasta(@Valid @RequestBody SubastaRequest request) {
        SubastaCreadaResponse subasta = subastaService.crearSubasta(request);

        return ResponseEntity
                .created(URI.create("/api/v1/subastas/" + subasta.id()))
                .body(subasta);
    }

    @Operation(summary = "Listar subastas del catálogo",
            description = "Todos los filtros son opcionales y combinables. `sort=menorTiempo` (default) "
                    + "ordena por fecha_fin ascendente; `sort=mayorPuja` ordena por oferta actual descendente.")
    @ApiResponse(responseCode = "200", description = "OK (lista vacía si nada matchea los filtros)")
    @GetMapping
    public ResponseEntity<List<SubastaListadoResponse>> listarSubastas(
            @Parameter(description = "Filtra por estado exacto") @RequestParam(required = false) EstadoSubasta estado,
            @Parameter(description = "Filtra por categoría") @RequestParam(required = false) Long categoriaId,
            @Parameter(description = "Precio base mínimo") @RequestParam(required = false) BigDecimal precioMin,
            @Parameter(description = "Precio base máximo") @RequestParam(required = false) BigDecimal precioMax,
            @Parameter(description = "menorTiempo (default) o mayorPuja")
            @RequestParam(required = false, defaultValue = "menorTiempo") String sort) {

        List<SubastaListadoResponse> subastas = subastaService.buscarSubastas(
                estado, categoriaId, precioMin, precioMax, sort);

        return ResponseEntity.ok(subastas);
    }

    @Operation(summary = "Listar categorías")
    @ApiResponse(responseCode = "200", description = "OK")
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(subastaService.obtenerCategorias());
    }

    @Operation(summary = "Detalle de una subasta",
            description = "Incluye la oferta actual (líder o precio base si nadie pujó todavía) y "
                    + "la cantidad de pujas recibidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Subasta inexistente",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<SubastaDetalleResponse> obtenerDetalle(
            @Parameter(description = "Id de la subasta") @PathVariable Long id) {
        return ResponseEntity.ok(subastaService.obtenerDetalle(id));
    }
}
