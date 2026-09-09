package com.unaj.subastaya.service;

import com.unaj.subastaya.dto.LoginRequest;
import com.unaj.subastaya.dto.LoginResponse;
import com.unaj.subastaya.exception.CredencialesInvalidasException;
import com.unaj.subastaya.model.Usuario;
import com.unaj.subastaya.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }

        return new LoginResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
