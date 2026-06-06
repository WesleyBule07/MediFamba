package com.medifamba.services;

import com.medifamba.dto.request.LoginRequest;
import com.medifamba.dto.request.RegisterRequest;
import com.medifamba.dto.response.AuthResponse;
import com.medifamba.dto.response.UtilizadorResponse;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.BusinessException;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.repositories.UtilizadorRepository;
import com.medifamba.security.jwt.JwtUtil;
import com.medifamba.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilizadorRepository utilizadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final UserDetailsServiceImpl userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (utilizadorRepository.existsByEmail(req.email()))
            throw new ConflictException("Email já registado: " + req.email());
        Utilizador u = Utilizador.builder()
                .nome(req.nome()).email(req.email())
                .senha(passwordEncoder.encode(req.senha()))
                .role(req.role() != null ? req.role() : UserRole.USER)
                .ativo(true).build();
        return buildResponse(utilizadorRepository.save(u));
    }

    public AuthResponse login(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.senha()));
        Utilizador u = utilizadorRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException("Utilizador não encontrado"));
        if (!u.isAtivo()) throw new BusinessException("Conta desativada.");
        return buildResponse(u);
    }

    private AuthResponse buildResponse(Utilizador u) {
        UserDetails ud = userDetailsService.loadUserByUsername(u.getEmail());
        return AuthResponse.of(
                jwtUtil.generateToken(ud),
                jwtUtil.generateRefreshToken(ud),
                new UtilizadorResponse(u.getId(), u.getNome(), u.getEmail(),
                        u.getRole(), u.isAtivo(), u.getFotoUrl(), u.getCreatedAt())
        );
    }
}