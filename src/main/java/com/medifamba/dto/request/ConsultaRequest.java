package com.medifamba.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaRequest(
        @NotNull UUID pacienteId,
        @NotNull UUID medicoId,
        @NotNull LocalDateTime dataHora,
        @NotBlank @Size(max=500) String motivo,
        String sala
) {}