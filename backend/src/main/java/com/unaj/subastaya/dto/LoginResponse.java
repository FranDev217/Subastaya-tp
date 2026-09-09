package com.unaj.subastaya.dto;

public record LoginResponse(
        Long usuarioId,
        String nombre,
        String email
) {
}
