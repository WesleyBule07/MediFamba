package com.medifamba.dto.response;
import com.medifamba.enums.NotificacaoTipo;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id, NotificacaoTipo tipo,
        String titulo, String mensagem,
        boolean lida, UUID consultaId,
        LocalDateTime createdAt
) {}