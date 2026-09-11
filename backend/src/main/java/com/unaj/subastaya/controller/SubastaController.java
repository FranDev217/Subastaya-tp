package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.CategoriaResponse;
import com.unaj.subastaya.dto.SubastaCreadaResponse;
import com.unaj.subastaya.dto.SubastaListadoResponse;
import com.unaj.subastaya.dto.SubastaRequest;
import com.unaj.subastaya.model.EstadoSubasta;
import com.unaj.subastaya.service.SubastaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
public class SubastaController {

    private final SubastaService subastaService;

    @PostMapping
    public ResponseEntity<SubastaCreadaResponse> crearSubasta(@Valid @RequestBody SubastaRequest request) {
        SubastaCreadaResponse subasta = subastaService.crearSubasta(request);

        return ResponseEntity
                .created(URI.create("/api/v1/subastas/" + subasta.id()))
                .body(subasta);
    }

    @GetMapping
    public ResponseEntity<List<SubastaListadoResponse>> listarSubastas(
            @RequestParam(required = false) EstadoSubasta estado,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @RequestParam(required = false, defaultValue = "menorTiempo") String sort) {

        List<SubastaListadoResponse> subastas = subastaService.buscarSubastas(
                estado, categoriaId, precioMin, precioMax, sort);

        return ResponseEntity.ok(subastas);
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaResponse>> listarCategorias() {
        return ResponseEntity.ok(subastaService.obtenerCategorias());
    }
}
