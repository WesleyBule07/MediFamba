package com.medifamba.services;

import com.medifamba.dto.request.PacienteRequest;
import com.medifamba.dto.response.PacienteResponse;
import com.medifamba.entities.Paciente;
import com.medifamba.entities.Utilizador;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.PacienteRepository;
import com.medifamba.repositories.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final AuditService auditService;

    @Transactional
    public PacienteResponse criar(PacienteRequest req, Utilizador actor) {
        if (req.numeroIdentificacao() != null &&
                pacienteRepository.existsByNumeroIdentificacao(req.numeroIdentificacao()))
            throw new ConflictException("Número de identificação já registado.");

        Utilizador utilizador = null;
        if (req.utilizadorId() != null)
            utilizador = utilizadorRepository.findById(req.utilizadorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilizador", req.utilizadorId()));

        Paciente p = Paciente.builder()
                .nomeCompleto(req.nomeCompleto())
                .dataNascimento(req.dataNascimento())
                .genero(req.genero())
                .telefone(req.telefone())
                .endereco(req.endereco())
                .contactoEmergencia(req.contactoEmergencia())
                .numeroIdentificacao(req.numeroIdentificacao())
                .historicoBasico(req.historicoBasico())
                .utilizador(utilizador).ativo(true).build();

        p = pacienteRepository.save(p);
        auditService.registrar(actor, "CRIAR_PACIENTE", "Paciente", p.getId(), p.getNomeCompleto());
        return toResponse(p);
    }

    @Transactional
    public PacienteResponse atualizar(UUID id, PacienteRequest req, Utilizador actor) {
        Paciente p = findById(id);
        if (req.numeroIdentificacao() != null
                && !req.numeroIdentificacao().equals(p.getNumeroIdentificacao())
                && pacienteRepository.existsByNumeroIdentificacao(req.numeroIdentificacao()))
            throw new ConflictException("Número de identificação já registado.");

        p.setNomeCompleto(req.nomeCompleto());
        p.setDataNascimento(req.dataNascimento());
        p.setGenero(req.genero());
        p.setTelefone(req.telefone());
        p.setEndereco(req.endereco());
        p.setContactoEmergencia(req.contactoEmergencia());
        p.setNumeroIdentificacao(req.numeroIdentificacao());
        p.setHistoricoBasico(req.historicoBasico());

        auditService.registrar(actor, "ATUALIZAR_PACIENTE", "Paciente", id, null);
        return toResponse(pacienteRepository.save(p));
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponse> listar(String q, Pageable pageable) {
        Page<Paciente> page = (q != null && !q.isBlank())
                ? pacienteRepository.search(q.trim(), pageable)
                : pacienteRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public void desativar(UUID id, Utilizador actor) {
        Paciente p = findById(id);
        p.setAtivo(false);
        pacienteRepository.save(p);
        auditService.registrar(actor, "DESATIVAR_PACIENTE", "Paciente", id, null);
    }

    private Paciente findById(UUID id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", id));
    }

    public PacienteResponse toResponse(Paciente p) {
        return new PacienteResponse(p.getId(), p.getNomeCompleto(), p.getDataNascimento(),
                p.getGenero(), p.getTelefone(), p.getEndereco(), p.getContactoEmergencia(),
                p.getNumeroIdentificacao(), p.getHistoricoBasico(), p.getFotoUrl(),
                p.isAtivo(), p.getCreatedAt(), p.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorUtilizador(UUID utilizadorId) {
        return pacienteRepository.findByUtilizadorId(utilizadorId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de paciente", utilizadorId));
    }
}