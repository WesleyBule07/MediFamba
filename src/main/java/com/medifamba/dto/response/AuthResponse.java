package com.medifamba.dto.response;
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UtilizadorResponse utilizador
) {
    public static AuthResponse of(String access, String refresh, UtilizadorResponse user) {
        return new AuthResponse(access, refresh, "Bearer", user);
    }
}