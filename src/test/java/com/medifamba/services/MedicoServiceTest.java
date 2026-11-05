package com.medifamba.services;

import com.medifamba.dto.request.MedicoRequest;
import com.medifamba.dto.response.MedicoResponse;
import com.medifamba.entities.Medico;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.MedicoRepository;
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

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock private MedicoRepository medicoRepository;
    @Mock private AuditService auditService;

    @InjectMocks private MedicoService medicoService;

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

    private Medico medico(UUID id) {
        return Medico.builder()
                .id(id)
                .nome("Dr. Joao")
                .especialidade("Cardiologia")
                .contacto("912345678")
                .numeroProfissional("MED-1")
                .horarioInicio(LocalTime.of(8, 0))
                .horarioFim(LocalTime.of(17, 0))
                .disponivel(true)
                .ativo(true)
                .build();
    }

    private MedicoRequest request(String numeroProfissional) {
        return new MedicoRequest("Dr. Joao", "Cardiologia", "912345678",
                numeroProfissional, LocalTime.of(8, 0), LocalTime.of(17, 0), true, null);
    }

    @Test
    void criar_deveGuardarMedicoEAuditar() {
        Utilizador actor = actor();
        when(medicoRepository.existsByNumeroProfissional("MED-1")).thenReturn(false);
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> {
            Medico m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        MedicoResponse response = medicoService.criar(request("MED-1"), actor);

        assertThat(response.id()).isNotNull();
        assertThat(response.nome()).isEqualTo("Dr. Joao");
        assertThat(response.ativo()).isTrue();

        ArgumentCaptor<Medico> captor = ArgumentCaptor.forClass(Medico.class);
        verify(medicoRepository).save(captor.capture());
        assertThat(captor.getValue().isAtivo()).isTrue();
        verify(auditService).registrar(eq(actor), eq("CRIAR_MEDICO"),
                eq("Medico"), any(UUID.class), eq("Dr. Joao"));
    }

    @Test
    void criar_deveLancarConflictQuandoNumeroProfissionalJaExiste() {
        when(medicoRepository.existsByNumeroProfissional("MED-1")).thenReturn(true);

        assertThatThrownBy(() -> medicoService.criar(request("MED-1"), actor()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Número profissional");

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void atualizar_deveAtualizarDadosEAuditar() {
        UUID id = UUID.randomUUID();
        Medico m = medico(id);
        when(medicoRepository.findById(id)).thenReturn(Optional.of(m));
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        MedicoResponse response = medicoService.atualizar(id, request("MED-1"), actor());

        assertThat(response.nome()).isEqualTo("Dr. Joao");
        verify(auditService).registrar(any(), eq("ATUALIZAR_MEDICO"),
                eq("Medico"), eq(id), isNull());
    }

    @Test
    void atualizar_deveLancarConflictQuandoNovoNumeroJaExiste() {
        UUID id = UUID.randomUUID();
        Medico m = medico(id);
        when(medicoRepository.findById(id)).thenReturn(Optional.of(m));
        when(medicoRepository.existsByNumeroProfissional("MED-2")).thenReturn(true);

        assertThatThrownBy(() -> medicoService.atualizar(id, request("MED-2"), actor()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void atualizar_deveLancarNotFoundQuandoMedicoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(medicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.atualizar(id, request("MED-1"), actor()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_deveUsarSearchQuandoFiltroInformado() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medico> page = new PageImpl<>(List.of(medico(UUID.randomUUID())), pageable, 1);
        when(medicoRepository.search("Joao", pageable)).thenReturn(page);

        Page<MedicoResponse> result = medicoService.listar("  Joao  ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(medicoRepository).search("Joao", pageable);
        verify(medicoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listar_deveListarTodosQuandoFiltroVazio() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Medico> page = new PageImpl<>(List.of(medico(UUID.randomUUID())), pageable, 1);
        when(medicoRepository.findAll(pageable)).thenReturn(page);

        Page<MedicoResponse> result = medicoService.listar(" ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(medicoRepository).findAll(pageable);
        verify(medicoRepository, never()).search(any(), any());
    }

    @Test
    void buscarPorId_deveLancarNotFoundQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(medicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Médico");
    }

    @Test
    void buscarPorUtilizador_deveRetornarMedico() {
        UUID utilizadorId = UUID.randomUUID();
        Medico m = medico(UUID.randomUUID());
        when(medicoRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.of(m));

        MedicoResponse response = medicoService.buscarPorUtilizador(utilizadorId);

        assertThat(response.numeroProfissional()).isEqualTo("MED-1");
    }

    @Test
    void buscarPorUtilizador_deveLancarNotFoundQuandoNaoExiste() {
        UUID utilizadorId = UUID.randomUUID();
        when(medicoRepository.findByUtilizadorId(utilizadorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.buscarPorUtilizador(utilizadorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Perfil de médico");
    }

    @Test
    void listarDisponiveis_deveMapearLista() {
        when(medicoRepository.findByDisponivelTrueAndAtivoTrue())
                .thenReturn(List.of(medico(UUID.randomUUID()), medico(UUID.randomUUID())));

        List<MedicoResponse> result = medicoService.listarDisponiveis();

        assertThat(result).hasSize(2);
    }

    @Test
    void desativar_deveMarcarComoInativoEAuditar() {
        UUID id = UUID.randomUUID();
        Medico m = medico(id);
        when(medicoRepository.findById(id)).thenReturn(Optional.of(m));
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        medicoService.desativar(id, actor());

        assertThat(m.isAtivo()).isFalse();
        verify(auditService).registrar(any(), eq("DESATIVAR_MEDICO"),
                eq("Medico"), eq(id), isNull());
    }

    @Test
    void desativar_deveLancarNotFoundQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(medicoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicoService.desativar(id, actor()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
