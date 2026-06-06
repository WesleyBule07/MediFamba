package com.medifamba.dto.response;
public record DashboardResponse(
        long totalPacientes,
        long totalMedicos,
        long consultasHoje,
        long consultasMes,
        long pendentes,
        long confirmadas,
        long concluidas,
        long canceladas,
        long medicosDisponiveis,
        long notificacoesNaoLidas
) {}