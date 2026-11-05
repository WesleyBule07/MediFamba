package com.medifamba.services;

import com.medifamba.dto.response.DashboardResponse;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.ConsultaEstado;
import com.medifamba.enums.UserRole;
import com.medifamba.repositories.ConsultaRepository;
import com.medifamba.repositories.MedicoRepository;
import com.medifamba.repositories.NotificacaoRepository;
import com.medifamba.repositories.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private PacienteRepository pacienteRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private NotificacaoRepository notificacaoRepository;

    @InjectMocks private DashboardService dashboardService;

    @Test
    void getDashboard_deveAgregarTodasAsMetricas() {
        Utilizador utilizador = Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .email("admin@medifamba.com")
                .senha("hash")
                .role(UserRole.ADMIN)
                .ativo(true)
                .build();

        when(pacienteRepository.countByAtivo(true)).thenReturn(10L);
        when(medicoRepository.countByAtivo(true)).thenReturn(4L);
        when(consultaRepository.countByDia(any(), any())).thenReturn(3L);
        when(consultaRepository.countByEstado(ConsultaEstado.PENDENTE)).thenReturn(5L);
        when(consultaRepository.countByEstado(ConsultaEstado.CONFIRMADA)).thenReturn(6L);
        when(consultaRepository.countByEstado(ConsultaEstado.CONCLUIDA)).thenReturn(7L);
        when(consultaRepository.countByEstado(ConsultaEstado.CANCELADA)).thenReturn(8L);
        when(medicoRepository.countByDisponivelTrue()).thenReturn(2L);
        when(notificacaoRepository.countByUtilizadorIdAndLidaFalse(utilizador.getId()))
                .thenReturn(9L);

        DashboardResponse response = dashboardService.getDashboard(utilizador);

        assertThat(response.totalPacientes()).isEqualTo(10L);
        assertThat(response.totalMedicos()).isEqualTo(4L);
        assertThat(response.consultasHoje()).isEqualTo(3L);
        assertThat(response.consultasMes()).isEqualTo(3L);
        assertThat(response.pendentes()).isEqualTo(5L);
        assertThat(response.confirmadas()).isEqualTo(6L);
        assertThat(response.concluidas()).isEqualTo(7L);
        assertThat(response.canceladas()).isEqualTo(8L);
        assertThat(response.medicosDisponiveis()).isEqualTo(2L);
        assertThat(response.notificacoesNaoLidas()).isEqualTo(9L);
    }
}
