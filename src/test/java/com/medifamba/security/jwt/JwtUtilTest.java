package com.medifamba.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET =
            "3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86_400_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604_800_000L);
    }

    private UserDetails user(String username) {
        return User.withUsername(username)
                .password("hash")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void generateToken_deveExtrairUsername() {
        String token = jwtUtil.generateToken(user("ana@medifamba.com"));

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("ana@medifamba.com");
    }

    @Test
    void isTokenValid_deveRetornarTrueParaMesmoUtilizador() {
        String token = jwtUtil.generateToken(user("ana@medifamba.com"));

        assertThat(jwtUtil.isTokenValid(token, user("ana@medifamba.com"))).isTrue();
    }

    @Test
    void isTokenValid_deveRetornarFalseParaUtilizadorDiferente() {
        String token = jwtUtil.generateToken(user("ana@medifamba.com"));

        assertThat(jwtUtil.isTokenValid(token, user("outro@medifamba.com"))).isFalse();
    }

    @Test
    void generateRefreshToken_deveSerValido() {
        String token = jwtUtil.generateRefreshToken(user("ana@medifamba.com"));

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("ana@medifamba.com");
        assertThat(jwtUtil.isTokenValid(token, user("ana@medifamba.com"))).isTrue();
    }
}
