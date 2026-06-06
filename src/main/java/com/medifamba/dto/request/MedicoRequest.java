package com.medifamba.dto.request;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.UUID;

public record MedicoRequest(
        @NotBlank @Size(min=2,max=150) String nome,
        @NotBlank String especialidade,
        String contacto,
        @NotBlank String numeroProfissional,
        @NotNull LocalTime horarioInicio,
        @NotNull LocalTime horarioFim,
        boolean disponivel,
        UUID utilizadorId
) {}