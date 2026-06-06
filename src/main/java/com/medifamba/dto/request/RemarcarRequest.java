package com.medifamba.dto.request;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record RemarcarRequest(
        @NotNull LocalDateTime novaDataHora,
        String motivo
) {}