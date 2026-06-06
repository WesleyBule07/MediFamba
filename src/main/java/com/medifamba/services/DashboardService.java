package com.medifamba.services;

import com.medifamba.dto.response.DashboardResponse;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.ConsultaEstado;
import com.medifamba.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final ConsultaRepository consultaRepository;
    private final NotificacaoRepository notificacaoRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Utilizador utilizador) {
        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime fimDia = inicioDia.plusDays(1);
        YearMonth mes = YearMonth.now();
        LocalDateTime inicioMes = mes.atDay(1).atStartOfDay();
        LocalDateTime fimMes = mes.atEndOfMonth().atTime(23, 59, 59);

        return new DashboardResponse(
                pacienteRepository.countByAtivo(true),
                medicoRepository.countByAtivo(true),
                consultaRepository.countByDia(inicioDia, fimDia),
                consultaRepository.countByDia(inicioMes, fimMes),
                consultaRepository.countByEstado(ConsultaEstado.PENDENTE),
                consultaRepository.countByEstado(ConsultaEstado.CONFIRMADA),
                consultaRepository.countByEstado(ConsultaEstado.CONCLUIDA),
                consultaRepository.countByEstado(ConsultaEstado.CANCELADA),
                medicoRepository.countByDisponivelTrue(),
                notificacaoRepository.countByUtilizadorIdAndLidaFalse(utilizador.getId())
        );
    }
}