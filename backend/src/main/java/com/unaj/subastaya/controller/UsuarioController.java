package com.unaj.subastaya.controller;

import com.unaj.subastaya.dto.MiCompraResponse;
import com.unaj.subastaya.dto.MiPublicacionResponse;
import com.unaj.subastaya.service.ActividadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final ActividadService actividadService;

    @GetMapping("/{usuarioId}/publicaciones")
    public ResponseEntity<List<MiPublicacionResponse>> misPublicaciones(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(actividadService.misPublicaciones(usuarioId));
    }

    @GetMapping("/{usuarioId}/compras")
    public ResponseEntity<List<MiCompraResponse>> misCompras(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(actividadService.misCompras(usuarioId));
    }
}
