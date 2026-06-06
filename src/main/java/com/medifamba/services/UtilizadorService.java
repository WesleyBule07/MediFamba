package com.medifamba.services;

import com.medifamba.dto.request.CriarUtilizadorComPerfilRequest;
import com.medifamba.dto.response.UtilizadorResponse;
import com.medifamba.entities.Medico;
import com.medifamba.entities.Paciente;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.exceptions.BusinessException;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.repositories.MedicoRepository;
import com.medifamba.repositories.PacienteRepository;
import com.medifamba.repositories.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public UtilizadorResponse criarComPerfil(CriarUtilizadorComPerfilRequest req,
                                             Utilizador actor) {
        if (utilizadorRepository.existsByEmail(req.email()))
            throw new ConflictException("Email já registado: " + req.email());

        Utilizador u = Utilizador.builder()
                .nome(req.nome())
                .email(req.email())
                .senha(passwordEncoder.encode(req.senha()))
                .role(req.role())
                .ativo(true)
                .build();
        u = utilizadorRepository.save(u);

        // Cria o perfil conforme o role
        if (req.role() == UserRole.DOCTOR) {
            if (req.numeroProfissional() == null || req.especialidade() == null)
                throw new BusinessException(
                        "Número profissional e especialidade são obrigatórios para médicos.");
            if (medicoRepository.existsByNumeroProfissional(req.numeroProfissional()))
                throw new ConflictException("Número profissional já registado.");
            medicoRepository.save(Medico.builder()
                    .utilizador(u)
                    .nome(req.nome())
                    .especialidade(req.especialidade())
                    .numeroProfissional(req.numeroProfissional())
                    .contacto(req.contacto())
                    .horarioInicio(req.horarioInicio() != null
                            ? req.horarioInicio() : java.time.LocalTime.of(8,0))
                    .horarioFim(req.horarioFim() != null
                            ? req.horarioFim() : java.time.LocalTime.of(17,0))
                    .disponivel(req.disponivel() != null ? req.disponivel() : true)
                    .ativo(true)
                    .build());
        }

        if (req.role() == UserRole.PATIENT) {
            if (req.dataNascimento() == null || req.genero() == null)
                throw new BusinessException(
                        "Data de nascimento e género são obrigatórios para pacientes.");
            pacienteRepository.save(Paciente.builder()
                    .utilizador(u)
                    .nomeCompleto(req.nome())
                    .dataNascimento(req.dataNascimento())
                    .genero(req.genero())
                    .telefone(req.telefone())
                    .endereco(req.endereco())
                    .contactoEmergencia(req.contactoEmergencia())
                    .numeroIdentificacao(req.numeroIdentificacao())
                    .historicoBasico(req.historicoBasico())
                    .ativo(true)
                    .build());
        }

        auditService.registrar(actor, "CRIAR_UTILIZADOR", "Utilizador",
                u.getId(), u.getRole().name() + ": " + u.getEmail());

        return toResponse(u);
    }

    @Transactional(readOnly = true)
    public Page<UtilizadorResponse> listar(Pageable pageable) {
        return utilizadorRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UtilizadorResponse alterarEstado(UUID id, boolean ativo, Utilizador actor) {
        Utilizador u = utilizadorRepository.findById(id)
                .orElseThrow(() -> new com.medifamba.exceptions
                        .ResourceNotFoundException("Utilizador", id));
        u.setAtivo(ativo);
        utilizadorRepository.save(u);
        auditService.registrar(actor, ativo ? "ATIVAR_UTILIZADOR" : "DESATIVAR_UTILIZADOR",
                "Utilizador", id, u.getEmail());
        return toResponse(u);
    }

    public UtilizadorResponse toResponse(Utilizador u) {
        return new UtilizadorResponse(u.getId(), u.getNome(), u.getEmail(),
                u.getRole(), u.isAtivo(), u.getFotoUrl(), u.getCreatedAt());
    }
}