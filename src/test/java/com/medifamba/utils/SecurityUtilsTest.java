package com.medifamba.utils;

import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.UtilizadorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    private static final String EMAIL = "ana@medifamba.com";

    @Mock private UtilizadorRepository utilizadorRepository;

    @InjectMocks private SecurityUtils securityUtils;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }

    @Test
    void getUtilizadorActual_deveRetornarUtilizadorAutenticado() {
        Utilizador u = Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Ana")
                .email(EMAIL)
                .senha("hash")
                .role(UserRole.PATIENT)
                .ativo(true)
                .build();
        autenticar(EMAIL);
        when(utilizadorRepository.findByEmail(EMAIL)).thenReturn(Optional.of(u));

        Utilizador result = securityUtils.getUtilizadorActual();

        assertThat(result).isEqualTo(u);
    }

    @Test
    void getUtilizadorActual_deveLancarQuandoUtilizadorNaoExiste() {
        autenticar(EMAIL);
        when(utilizadorRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> securityUtils.getUtilizadorActual())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(EMAIL);
    }
}
