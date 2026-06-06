package com.medifamba.dto.request;
import com.medifamba.enums.Genero;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record PacienteRequest(
        @NotBlank @Size(min=2,max=150) String nomeCompleto,
        @NotNull LocalDate dataNascimento,
        @NotNull Genero genero,
        String telefone,
        String endereco,
        String contactoEmergencia,
        String numeroIdentificacao,
        String historicoBasico,
        UUID utilizadorId
) {}