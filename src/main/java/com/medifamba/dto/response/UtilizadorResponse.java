package com.medifamba.dto.response;
import com.medifamba.enums.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;

public record UtilizadorResponse(
        UUID id, String nome, String email,
        UserRole role, boolean ativo, String fotoUrl,
        LocalDateTime createdAt
) {}