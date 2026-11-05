package com.medifamba.services;

import com.medifamba.dto.request.PacienteRequest;
import com.medifamba.dto.response.PacienteResponse;
import com.medifamba.entities.Paciente;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.Genero;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.PacienteRepository;
import com.medifamba.repositories.UtilizadorRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock private PacienteRepository pacienteRepository;
    @Mock private UtilizadorRepository utilizadorRepository;
    @Mock private AuditService auditService;

    @InjectMocks private PacienteService pacienteService;

    private Utilizador actor() {
        return Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .email("admin@medifamba.com")
                .senha("hash")
                .role(UserRole.ADMIN)
                .ativo(true)
                .build();
    }

    private Paciente paciente(UUID id) {
        return Paciente.builder()
                .id(id)
                .nomeCompleto("Maria Silva")
                .dataNascimento(LocalDate.of(1990, 1, 1))
                .genero(Genero.FEMININO)
                .telefone("912345678")
                .numeroIdentificacao("ID-1")
                .ativo(true)
                .build();
    }

    private PacienteRequest request(String numeroIdentificacao, UUID utilizadorId) {
        return new PacienteRequest("Maria Silva", LocalDate.of(1990, 1, 1),
                Genero.FEMININO, "912345678", "Rua A", "911111111",
                numeroIdentificacao, "Sem historico", utilizadorId);
    }

    @Test
    void criar_deveGuardarPacienteEAuditar() {
        Utilizador actor = actor();
        when(pacienteRepository.existsByNumeroIdentificacao("ID-1")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        PacienteResponse response = pacienteService.criar(request("ID-1", null), actor);

        assertThat(response.id()).isNotNull();
        assertThat(response.nomeCompleto()).isEqualTo("Maria Silva");
        assertThat(response.ativo()).isTrue();
        verify(auditService).registrar(eq(actor), eq("CRIAR_PACIENTE"),
                eq("Paciente"), any(UUID.class), eq("Maria Silva"));
    }

    @Test
    void criar_deveAssociarUtilizadorQuandoInformado() {
        UUID utilizadorId = UUID.randomUUID();
        Utilizador u = actor();
        when(pacienteRepository.existsByNumeroIdentificacao("ID-1")).thenReturn(false);
        when(utilizadorRepository.findById(utilizadorId)).thenReturn(Optional.of(u));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        pacienteService.criar(request("ID-1", utilizadorId), actor());

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(captor.capture());
        assertThat(captor.getValue().getUtilizador()).isEqualTo(u);
    }

    @Test
    void criar_deveLancarConflictQuandoNumeroIdentificacaoJaExiste() {
        when(pacienteRepository.existsByNumeroIdentificacao("ID-1")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.criar(request("ID-1", null), actor()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Número de identificação");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void criar_deveLancarNotFoundQuandoUtilizadorNaoExiste() {
        UUID utilizadorId = UUID.randomUUID();
        when(pacienteRepository.existsByNumeroIdentificacao("ID-1")).thenReturn(false);
        when(utilizadorRepository.findById(utilizadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.criar(request("ID-1", utilizadorId), actor()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilizador");
    }

    @Test
    void atualizar_deveAtualizarDadosEAuditar() {
        UUID id = UUID.randomUUID();
        Paciente p = paciente(id);
        when(pacienteRepository.findById(id)).thenReturn(Optional.of(p));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        PacienteResponse response = pacienteService.atualizar(id,
                request("ID-1", null), actor());

        assertThat(response.nomeCompleto()).isEqualTo("Maria Silva");
        verify(auditService).registrar(any(), eq("ATUALIZAR_PACIENTE"),
                eq("Paciente"), eq(id), isNull());
    }

    @Test
    void atualizar_deveLancarConflictQuandoNovoNumeroJaExiste() {
        UUID id = UUID.randomUUID();
        Paciente p = paciente(id);
        when(pacienteRepository.findById(id)).thenReturn(Optional.of(p));
        when(pacienteRepository.existsByNumeroIdentificacao("ID-2")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.atualizar(id,
                request("ID-2", null), actor()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void atualizar_deveLancarNotFoundQuandoPacienteNaoExiste() {
        UUID id = UUID.randomUUID();
        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.atualizar(id, request("ID-1", null), actor()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscarPorId_deveLancarNotFoundQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(pacienteRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente");
    }

    @Test
    void listar_deveUsarSearchQuandoFiltroInformado() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Paciente> page = new PageImpl<>(List.of(paciente(UUID.randomUUID())), pageable, 1);
        when(pacienteRepository.search("Maria", pageable)).thenReturn(page);

        Page<PacienteResponse> result = pacienteService.listar(" Maria ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(pacienteRepository).search("Maria", pageable);
        verify(pacienteRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listar_deveListarTodosQuandoFiltroVazio() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Paciente> page = new PageImpl<>(List.of(paciente(UUID.randomUUID())), pageable, 1);
        when(pacienteRepository.findAll(pageable)).thenReturn(page);

        Page<PacienteResponse> result = pacienteService.listar(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(pacienteRepository).findAll(pageable);
        verify(pacienteRepository, never()).search(any(), any());
    }

    @Test
    void desativar_deveMarcarComoInativoEAuditar() {
        UUID id = UUID.randomUUID();
        Paciente p = paciente(id);
        when(pacienteRepository.findById(id)).thenReturn(Optional.of(p));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        pacienteService.desativar(id, actor());

        assertThat(p.isAtivo()).isFalse();
        verify(auditService).registrar(any(), eq("DESATIVAR_PACIENTE"),
                eq("Paciente"), eq(id), isNull());
    }

    @Test
    void buscarPorUtilizador_deveRetornarPaciente() {
        UUID utilizadorId = UUID.randomUUID();
        Paciente p = paciente(UUID.randomUUID());
        when(pacienteRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.of(p));

        PacienteResponse response = pacienteService.buscarPorUtilizador(utilizadorId);

        assertThat(response.nomeCompleto()).isEqualTo("Maria Silva");
    }

    @Test
    void buscarPorUtilizador_deveLancarNotFoundQuandoNaoExiste() {
        UUID utilizadorId = UUID.randomUUID();
        when(pacienteRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorUtilizador(utilizadorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Perfil de paciente");
    }
}
