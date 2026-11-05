package com.medifamba.services;

import com.medifamba.dto.request.LoginRequest;
import com.medifamba.dto.request.RegisterRequest;
import com.medifamba.dto.response.AuthResponse;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.BusinessException;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.repositories.UtilizadorRepository;
import com.medifamba.security.jwt.JwtUtil;
import com.medifamba.security.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UtilizadorRepository utilizadorRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authManager;
    @Mock private UserDetailsServiceImpl userDetailsService;

    @InjectMocks private AuthService authService;

    private static final String EMAIL = "ana@medifamba.com";
    private static final String SENHA = "password123";

    private UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername(EMAIL)
                .password("hash")
                .authorities("ROLE_USER")
                .build();
    }

    private Utilizador utilizador(boolean ativo) {
        return Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Ana")
                .email(EMAIL)
                .senha("hash")
                .role(UserRole.USER)
                .ativo(ativo)
                .build();
    }

    @Test
    void register_deveCriarUtilizadorComRolePadraoQuandoRoleNula() {
        var req = new RegisterRequest("Ana", EMAIL, SENHA, null);
        when(utilizadorRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(SENHA)).thenReturn("hash");
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(inv -> {
            Utilizador u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");

        AuthResponse response = authService.register(req);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.utilizador().email()).isEqualTo(EMAIL);

        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(captor.capture());
        assertThat(captor.getValue().getSenha()).isEqualTo("hash");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(captor.getValue().isAtivo()).isTrue();
    }

    @Test
    void register_deveRespeitarRoleInformada() {
        var req = new RegisterRequest("Dr. Joao", EMAIL, SENHA, UserRole.DOCTOR);
        when(utilizadorRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(SENHA)).thenReturn("hash");
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(inv -> {
            Utilizador u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");

        authService.register(req);

        ArgumentCaptor<Utilizador> captor = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.DOCTOR);
    }

    @Test
    void register_deveLancarConflictQuandoEmailJaExiste() {
        var req = new RegisterRequest("Ana", EMAIL, SENHA, null);
        when(utilizadorRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(EMAIL);

        verify(utilizadorRepository, never()).save(any());
    }

    @Test
    void login_deveAutenticarERetornarTokens() {
        var req = new LoginRequest(EMAIL, SENHA);
        when(utilizadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilizador(true)));
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails());
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access");
        when(jwtUtil.generateRefreshToken(any(UserDetails.class))).thenReturn("refresh");

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.utilizador().email()).isEqualTo(EMAIL);
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_deveLancarBusinessExceptionQuandoContaDesativada() {
        var req = new LoginRequest(EMAIL, SENHA);
        when(utilizadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(utilizador(false)));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desativada");
    }

    @Test
    void login_deveLancarBusinessExceptionQuandoUtilizadorNaoExiste() {
        var req = new LoginRequest(EMAIL, SENHA);
        when(utilizadorRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não encontrado");
    }
}
