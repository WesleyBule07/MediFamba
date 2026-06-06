package com.medifamba.dto.response;
import com.medifamba.enums.Genero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PacienteResponse(
        UUID id, String nomeCompleto, LocalDate dataNascimento,
        Genero genero, String telefone, String endereco,
        String contactoEmergencia, String numeroIdentificacao,
        String historicoBasico, String fotoUrl,
        boolean ativo, LocalDateTime createdAt, LocalDateTime updatedAt
) {}