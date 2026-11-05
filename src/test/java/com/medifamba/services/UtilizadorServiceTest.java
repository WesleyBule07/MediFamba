package com.medifamba.services;

import com.medifamba.dto.request.CriarUtilizadorComPerfilRequest;
import com.medifamba.dto.response.UtilizadorResponse;
import com.medifamba.entities.Medico;
import com.medifamba.entities.Paciente;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.Genero;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.BusinessException;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.MedicoRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
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
class UtilizadorServiceTest {

    @Mock private UtilizadorRepository utilizadorRepository;
    @Mock private MedicoRepository medicoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    @InjectMocks private UtilizadorService utilizadorService;

    private static final String EMAIL = "novo@medifamba.com";

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

    private CriarUtilizadorComPerfilRequest request(UserRole role, String especialidade,
                                                    String numeroProfissional,
                                                    LocalDate dataNascimento, Genero genero) {
        return new CriarUtilizadorComPerfilRequest("Novo", EMAIL, "password123", role,
                especialidade, numeroProfissional, "912345678",
                null, null, null,
                dataNascimento, genero, "912345678", "Rua A", "911111111", "ID-1", null);
    }

    private void stubSaveUtilizador() {
        when(utilizadorRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(inv -> {
            Utilizador u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
    }

    @Test
    void criarComPerfil_deveLancarConflictQuandoEmailJaExiste() {
        when(utilizadorRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> utilizadorService.criarComPerfil(
                request(UserRole.USER, null, null, null, null), actor()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining(EMAIL);

        verify(utilizadorRepository, never()).save(any());
    }

    @Test
    void criarComPerfil_deveCriarUtilizadorSemPerfilParaRoleAdmin() {
        stubSaveUtilizador();

        UtilizadorResponse response = utilizadorService.criarComPerfil(
                request(UserRole.ADMIN, null, null, null, null), actor());

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
        verify(medicoRepository, never()).save(any());
        verify(pacienteRepository, never()).save(any());
        verify(auditService).registrar(any(), eq("CRIAR_UTILIZADOR"),
                eq("Utilizador"), any(UUID.class), eq("ADMIN: " + EMAIL));
    }

    @Test
    void criarComPerfil_deveLancarBusinessExceptionParaMedicoSemDadosObrigatorios() {
        stubSaveUtilizador();

        assertThatThrownBy(() -> utilizadorService.criarComPerfil(
                request(UserRole.DOCTOR, null, null, null, null), actor()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Número profissional e especialidade");

        verify(medicoRepository, never()).save(any());
    }

    @Test
    void criarComPerfil_deveLancarConflictQuandoNumeroProfissionalJaExiste() {
        stubSaveUtilizador();
        when(medicoRepository.existsByNumeroProfissional("MED-1")).thenReturn(true);

        assertThatThrownBy(() -> utilizadorService.criarComPerfil(
                request(UserRole.DOCTOR, "Cardiologia", "MED-1", null, null), actor()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Número profissional");
    }

    @Test
    void criarComPerfil_deveCriarMedicoComHorariosPadrao() {
        stubSaveUtilizador();
        when(medicoRepository.existsByNumeroProfissional("MED-1")).thenReturn(false);
        when(medicoRepository.save(any(Medico.class))).thenAnswer(inv -> inv.getArgument(0));

        utilizadorService.criarComPerfil(
                request(UserRole.DOCTOR, "Cardiologia", "MED-1", null, null), actor());

        ArgumentCaptor<Medico> captor = ArgumentCaptor.forClass(Medico.class);
        verify(medicoRepository).save(captor.capture());
        Medico m = captor.getValue();
        assertThat(m.getNome()).isEqualTo("Novo");
        assertThat(m.getEspecialidade()).isEqualTo("Cardiologia");
        assertThat(m.getHorarioInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(m.getHorarioFim()).isEqualTo(LocalTime.of(17, 0));
        assertThat(m.isDisponivel()).isTrue();
        assertThat(m.isAtivo()).isTrue();
    }

    @Test
    void criarComPerfil_deveLancarBusinessExceptionParaPacienteSemDadosObrigatorios() {
        stubSaveUtilizador();

        assertThatThrownBy(() -> utilizadorService.criarComPerfil(
                request(UserRole.PATIENT, null, null, null, null), actor()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Data de nascimento e género");

        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void criarComPerfil_deveCriarPaciente() {
        stubSaveUtilizador();
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        utilizadorService.criarComPerfil(
                request(UserRole.PATIENT, null, null, LocalDate.of(1990, 1, 1), Genero.FEMININO),
                actor());

        ArgumentCaptor<Paciente> captor = ArgumentCaptor.forClass(Paciente.class);
        verify(pacienteRepository).save(captor.capture());
        assertThat(captor.getValue().getNomeCompleto()).isEqualTo("Novo");
        assertThat(captor.getValue().getGenero()).isEqualTo(Genero.FEMININO);
        assertThat(captor.getValue().isAtivo()).isTrue();
    }

    @Test
    void listar_deveMapearPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        Utilizador u = actor();
        Page<Utilizador> page = new PageImpl<>(List.of(u), pageable, 1);
        when(utilizadorRepository.findAll(pageable)).thenReturn(page);

        Page<UtilizadorResponse> result = utilizadorService.listar(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).email()).isEqualTo("admin@medifamba.com");
    }

    @Test
    void alterarEstado_deveAtualizarEstadoEAuditar() {
        UUID id = UUID.randomUUID();
        Utilizador u = actor();
        u.setAtivo(false);
        when(utilizadorRepository.findById(id)).thenReturn(Optional.of(u));
        when(utilizadorRepository.save(any(Utilizador.class))).thenAnswer(inv -> inv.getArgument(0));

        UtilizadorResponse response = utilizadorService.alterarEstado(id, true, actor());

        assertThat(response.ativo()).isTrue();
        verify(auditService).registrar(any(), eq("ATIVAR_UTILIZADOR"),
                eq("Utilizador"), eq(id), eq(u.getEmail()));
    }

    @Test
    void alterarEstado_deveLancarNotFoundQuandoUtilizadorNaoExiste() {
        UUID id = UUID.randomUUID();
        when(utilizadorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilizadorService.alterarEstado(id, false, actor()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Utilizador");
    }
}
