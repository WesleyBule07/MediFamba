package com.medifamba.security.service;

import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.repositories.UtilizadorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UtilizadorRepository utilizadorRepository;

    @InjectMocks private UserDetailsServiceImpl userDetailsService;

    private Utilizador utilizador(boolean ativo) {
        return Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Ana")
                .email("ana@medifamba.com")
                .senha("hash")
                .role(UserRole.DOCTOR)
                .ativo(ativo)
                .build();
    }

    @Test
    void loadUserByUsername_deveMapearUtilizadorParaUserDetails() {
        when(utilizadorRepository.findByEmail("ana@medifamba.com"))
                .thenReturn(Optional.of(utilizador(true)));

        UserDetails result = userDetailsService.loadUserByUsername("ana@medifamba.com");

        assertThat(result.getUsername()).isEqualTo("ana@medifamba.com");
        assertThat(result.getPassword()).isEqualTo("hash");
        assertThat(result.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_DOCTOR");
        assertThat(result.isAccountNonLocked()).isTrue();
    }

    @Test
    void loadUserByUsername_deveBloquearContaInativa() {
        when(utilizadorRepository.findByEmail("ana@medifamba.com"))
                .thenReturn(Optional.of(utilizador(false)));

        UserDetails result = userDetailsService.loadUserByUsername("ana@medifamba.com");

        assertThat(result.isAccountNonLocked()).isFalse();
    }

    @Test
    void loadUserByUsername_deveLancarQuandoUtilizadorNaoExiste() {
        when(utilizadorRepository.findByEmail("ana@medifamba.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ana@medifamba.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ana@medifamba.com");
    }
}
