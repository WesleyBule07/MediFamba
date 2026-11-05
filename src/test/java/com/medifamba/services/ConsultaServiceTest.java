package com.medifamba.services;

import com.medifamba.dto.request.AlterarEstadoRequest;
import com.medifamba.dto.request.ConsultaRequest;
import com.medifamba.dto.request.RemarcarRequest;
import com.medifamba.dto.response.ConsultaResponse;
import com.medifamba.entities.Consulta;
import com.medifamba.entities.Medico;
import com.medifamba.entities.Notificacao;
import com.medifamba.entities.Paciente;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.ConsultaEstado;
import com.medifamba.enums.NotificacaoTipo;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.BusinessException;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.ConsultaRepository;
import com.medifamba.repositories.MedicoRepository;
import com.medifamba.repositories.NotificacaoRepository;
import com.medifamba.repositories.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock private ConsultaRepository consultaRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private NotificacaoRepository notificacaoRepository;
    @Mock private AuditService auditService;

    @InjectMocks private ConsultaService consultaService;

    private Utilizador utilizador() {
        return Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Utilizador")
                .email("u@medifamba.com")
                .senha("hash")
                .role(UserRole.ADMIN)
                .ativo(true)
                .build();
    }

    private Paciente paciente(UUID id) {
        return Paciente.builder()
                .id(id)
                .nomeCompleto("Maria Silva")
                .telefone("912345678")
                .dataNascimento(LocalDate.of(1990, 1, 1))
                .build();
    }

    private Medico medico(UUID id, Utilizador u) {
        return Medico.builder()
                .id(id)
                .nome("Dr. Joao")
                .especialidade("Cardiologia")
                .numeroProfissional("MED-1")
                .horarioInicio(LocalTime.of(8, 0))
                .horarioFim(LocalTime.of(17, 0))
                .disponivel(true)
                .ativo(true)
                .utilizador(u)
                .build();
    }

    private Consulta consulta(UUID id, Paciente p, Medico m, LocalDateTime dataHora) {
        return Consulta.builder()
                .id(id)
                .paciente(p)
                .medico(m)
                .dataHora(dataHora)
                .motivo("Consulta")
                .estado(ConsultaEstado.PENDENTE)
                .build();
    }

    private void stubSaveConsulta() {
        when(consultaRepository.save(any(Consulta.class))).thenAnswer(inv -> {
            Consulta c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
    }

    @Test
    void agendar_deveCriarConsultaNotificarEAuditar() {
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        LocalDateTime dataHora = LocalDate.of(2026, 1, 5).atTime(10, 0);
        Utilizador actor = utilizador();
        Paciente p = paciente(pacienteId);
        Medico m = medico(medicoId, utilizador());
        var req = new ConsultaRequest(pacienteId, medicoId, dataHora, "Rotina", "Sala 1");

        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.of(p));
        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(m));
        when(consultaRepository.existeConflito(medicoId, dataHora, null)).thenReturn(false);
        stubSaveConsulta();

        ConsultaResponse response = consultaService.agendar(req, actor);

        assertThat(response.id()).isNotNull();
        assertThat(response.estado()).isEqualTo(ConsultaEstado.PENDENTE);
        assertThat(response.paciente().nomeCompleto()).isEqualTo("Maria Silva");
        assertThat(response.medico().nome()).isEqualTo("Dr. Joao");

        ArgumentCaptor<Notificacao> notificacao = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(notificacao.capture());
        assertThat(notificacao.getValue().getTipo()).isEqualTo(NotificacaoTipo.NOVA_CONSULTA);
        assertThat(notificacao.getValue().getUtilizador()).isEqualTo(m.getUtilizador());

        verify(auditService).registrar(eq(actor), eq("AGENDAR_CONSULTA"),
                eq("Consulta"), any(UUID.class), isNull());
    }

    @Test
    void agendar_deveLancarNotFoundQuandoPacienteNaoExiste() {
        UUID pacienteId = UUID.randomUUID();
        var req = new ConsultaRequest(pacienteId, UUID.randomUUID(),
                LocalDate.of(2026, 1, 5).atTime(10, 0), "Rotina", null);
        when(pacienteRepository.findById(pacienteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.agendar(req, utilizador()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente");
    }

    @Test
    void agendar_deveLancarNotFoundQuandoMedicoNaoExiste() {
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        var req = new ConsultaRequest(pacienteId, medicoId,
                LocalDate.of(2026, 1, 5).atTime(10, 0), "Rotina", null);
        when(pacienteRepository.findById(pacienteId))
                .thenReturn(Optional.of(paciente(pacienteId)));
        when(medicoRepository.findById(medicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.agendar(req, utilizador()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Médico");
    }

    @Test
    void agendar_deveLancarBusinessExceptionForaDoHorario() {
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        var req = new ConsultaRequest(pacienteId, medicoId,
                LocalDate.of(2026, 1, 5).atTime(7, 0), "Rotina", null);
        when(pacienteRepository.findById(pacienteId))
                .thenReturn(Optional.of(paciente(pacienteId)));
        when(medicoRepository.findById(medicoId))
                .thenReturn(Optional.of(medico(medicoId, null)));

        assertThatThrownBy(() -> consultaService.agendar(req, utilizador()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Horário");
    }

    @Test
    void agendar_deveLancarConflictQuandoHorarioOcupado() {
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        LocalDateTime dataHora = LocalDate.of(2026, 1, 5).atTime(10, 0);
        var req = new ConsultaRequest(pacienteId, medicoId, dataHora, "Rotina", null);
        when(pacienteRepository.findById(pacienteId))
                .thenReturn(Optional.of(paciente(pacienteId)));
        when(medicoRepository.findById(medicoId))
                .thenReturn(Optional.of(medico(medicoId, null)));
        when(consultaRepository.existeConflito(medicoId, dataHora, null)).thenReturn(true);

        assertThatThrownBy(() -> consultaService.agendar(req, utilizador()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já tem consulta");
    }

    @Test
    void alterarEstado_deveAtualizarNotificarCANCELAMENTO() {
        UUID id = UUID.randomUUID();
        Paciente p = paciente(UUID.randomUUID());
        p.setUtilizador(utilizador());
        Consulta c = consulta(id, p, medico(UUID.randomUUID(), null),
                LocalDate.of(2026, 1, 5).atTime(10, 0));
        when(consultaRepository.findById(id)).thenReturn(Optional.of(c));
        stubSaveConsulta();

        ConsultaResponse response = consultaService.alterarEstado(id,
                new AlterarEstadoRequest(ConsultaEstado.CANCELADA, "Paciente desmarcou"),
                utilizador());

        assertThat(response.estado()).isEqualTo(ConsultaEstado.CANCELADA);
        assertThat(response.observacoes()).isEqualTo("Paciente desmarcou");

        ArgumentCaptor<Notificacao> notificacao = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(notificacao.capture());
        assertThat(notificacao.getValue().getTipo()).isEqualTo(NotificacaoTipo.CANCELAMENTO);
        verify(auditService).registrar(any(), eq("ALTERAR_ESTADO"), eq("Consulta"),
                eq(id), eq("CANCELADA"));
    }

    @Test
    void alterarEstado_naoDeveNotificarQuandoPacienteSemUtilizador() {
        UUID id = UUID.randomUUID();
        Consulta c = consulta(id, paciente(UUID.randomUUID()), medico(UUID.randomUUID(), null),
                LocalDate.of(2026, 1, 5).atTime(10, 0));
        when(consultaRepository.findById(id)).thenReturn(Optional.of(c));
        stubSaveConsulta();

        consultaService.alterarEstado(id,
                new AlterarEstadoRequest(ConsultaEstado.CONFIRMADA, null), utilizador());

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void alterarEstado_deveLancarNotFoundQuandoConsultaNaoExiste() {
        UUID id = UUID.randomUUID();
        when(consultaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.alterarEstado(id,
                new AlterarEstadoRequest(ConsultaEstado.CONFIRMADA, null), utilizador()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Consulta");
    }

    @Test
    void remarcar_deveAtualizarDataEstadoENotificar() {
        UUID id = UUID.randomUUID();
        Paciente p = paciente(UUID.randomUUID());
        p.setUtilizador(utilizador());
        Consulta c = consulta(id, p, medico(UUID.randomUUID(), null),
                LocalDate.of(2026, 1, 5).atTime(10, 0));
        LocalDateTime novaData = LocalDate.of(2026, 1, 6).atTime(11, 0);
        when(consultaRepository.findById(id)).thenReturn(Optional.of(c));
        when(consultaRepository.existeConflito(c.getMedico().getId(), novaData, id))
                .thenReturn(false);
        stubSaveConsulta();

        ConsultaResponse response = consultaService.remarcar(id,
                new RemarcarRequest(novaData, "Reagendado"), utilizador());

        assertThat(response.dataHora()).isEqualTo(novaData);
        assertThat(response.estado()).isEqualTo(ConsultaEstado.REMARCADA);
        assertThat(response.observacoes()).isEqualTo("Reagendado");

        ArgumentCaptor<Notificacao> notificacao = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(notificacao.capture());
        assertThat(notificacao.getValue().getTipo()).isEqualTo(NotificacaoTipo.ALTERACAO_HORARIO);
    }

    @Test
    void remarcar_deveLancarConflictQuandoNovoHorarioOcupado() {
        UUID id = UUID.randomUUID();
        Consulta c = consulta(id, paciente(UUID.randomUUID()), medico(UUID.randomUUID(), null),
                LocalDate.of(2026, 1, 5).atTime(10, 0));
        LocalDateTime novaData = LocalDate.of(2026, 1, 6).atTime(11, 0);
        when(consultaRepository.findById(id)).thenReturn(Optional.of(c));
        when(consultaRepository.existeConflito(c.getMedico().getId(), novaData, id))
                .thenReturn(true);

        assertThatThrownBy(() -> consultaService.remarcar(id,
                new RemarcarRequest(novaData, null), utilizador()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listarHoje_deveFiltrarPorEstadoQuandoInformado() {
        Consulta c = consulta(UUID.randomUUID(), paciente(UUID.randomUUID()),
                medico(UUID.randomUUID(), null), LocalDate.now().atTime(10, 0));
        when(consultaRepository.findByDiaAndEstado(any(), any(), eq(ConsultaEstado.PENDENTE)))
                .thenReturn(List.of(c));

        List<ConsultaResponse> result = consultaService.listarHoje(ConsultaEstado.PENDENTE);

        assertThat(result).hasSize(1);
        verify(consultaRepository).findByDiaAndEstado(any(), any(), eq(ConsultaEstado.PENDENTE));
        verify(consultaRepository, never()).findByDia(any(), any());
    }

    @Test
    void listarHoje_deveListarTodasQuandoEstadoNulo() {
        Consulta c = consulta(UUID.randomUUID(), paciente(UUID.randomUUID()),
                medico(UUID.randomUUID(), null), LocalDate.now().atTime(10, 0));
        when(consultaRepository.findByDia(any(), any())).thenReturn(List.of(c));

        List<ConsultaResponse> result = consultaService.listarHoje(null);

        assertThat(result).hasSize(1);
        verify(consultaRepository).findByDia(any(), any());
        verify(consultaRepository, never()).findByDiaAndEstado(any(), any(), any());
    }

    @Test
    void listarPorPaciente_deveMapearPagina() {
        UUID pacienteId = UUID.randomUUID();
        Consulta c = consulta(UUID.randomUUID(), paciente(pacienteId),
                medico(UUID.randomUUID(), null), LocalDate.now().atTime(10, 0));
        Pageable pageable = PageRequest.of(0, 10);
        Page<Consulta> page = new PageImpl<>(List.of(c), pageable, 1);
        when(consultaRepository.findByPacienteIdOrderByDataHoraDesc(pacienteId, pageable))
                .thenReturn(page);

        Page<ConsultaResponse> result = consultaService.listarPorPaciente(pacienteId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).paciente().id()).isEqualTo(pacienteId);
    }

    @Test
    void buscarPorId_deveLancarNotFoundQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(consultaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void horariosDisponiveis_deveGerarSlotsDe30MinutosExcluindoOcupados() {
        UUID medicoId = UUID.randomUUID();
        LocalDate data = LocalDate.of(2026, 1, 5);
        Medico m = medico(medicoId, null);
        m.setHorarioFim(LocalTime.of(10, 0));
        Consulta ocupada = consulta(UUID.randomUUID(), paciente(UUID.randomUUID()), m,
                data.atTime(9, 0));

        when(medicoRepository.findById(medicoId)).thenReturn(Optional.of(m));
        when(consultaRepository.findByMedicoAndDia(eq(medicoId), any(), any()))
                .thenReturn(List.of(ocupada));

        List<LocalDateTime> result = consultaService.horariosDisponiveis(medicoId, data);

        assertThat(result).containsExactly(
                data.atTime(8, 0),
                data.atTime(8, 30),
                data.atTime(9, 30));
    }

    @Test
    void horariosDisponiveis_deveLancarNotFoundQuandoMedicoNaoExiste() {
        UUID medicoId = UUID.randomUUID();
        when(medicoRepository.findById(medicoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.horariosDisponiveis(medicoId, LocalDate.now()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarHojeParaMedico_deveBuscarMedicoPeloUtilizador() {
        UUID utilizadorId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        Medico m = medico(medicoId, null);
        Consulta c = consulta(UUID.randomUUID(), paciente(UUID.randomUUID()), m,
                LocalDate.now().atTime(10, 0));
        when(medicoRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.of(m));
        when(consultaRepository.findByMedicoAndDia(eq(medicoId), any(), any()))
                .thenReturn(List.of(c));

        List<ConsultaResponse> result = consultaService.listarHojeParaMedico(utilizadorId);

        assertThat(result).hasSize(1);
    }

    @Test
    void listarHojeParaMedico_deveLancarNotFoundQuandoPerfilNaoExiste() {
        UUID utilizadorId = UUID.randomUUID();
        when(medicoRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.listarHojeParaMedico(utilizadorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Perfil de médico");
    }
}
