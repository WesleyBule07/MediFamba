package com.medifamba.dto.response;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record MedicoResponse(
        UUID id, String nome, String especialidade,
        String contacto, String numeroProfissional,
        LocalTime horarioInicio, LocalTime horarioFim,
        boolean disponivel, String fotoUrl,
        boolean ativo, LocalDateTime createdAt
) {}