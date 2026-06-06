package com.medifamba.dto.response;
import com.medifamba.enums.ConsultaEstado;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaResponse(
        UUID id,
        PacienteSummary paciente,
        MedicoSummary medico,
        LocalDateTime dataHora,
        String motivo, String sala,
        ConsultaEstado estado,
        String observacoes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record PacienteSummary(UUID id, String nomeCompleto, String telefone, String fotoUrl) {}
    public record MedicoSummary(UUID id, String nome, String especialidade, boolean disponivel, String fotoUrl) {}
}