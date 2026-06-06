package com.medifamba.services;

import com.medifamba.dto.request.MedicoRequest;
import com.medifamba.dto.response.MedicoResponse;
import com.medifamba.entities.Medico;
import com.medifamba.entities.Utilizador;
import com.medifamba.exceptions.ConflictException;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.MedicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicoService {

    private final MedicoRepository medicoRepository;
    private final AuditService auditService;

    @Transactional
    public MedicoResponse criar(MedicoRequest req, Utilizador actor) {
        if (medicoRepository.existsByNumeroProfissional(req.numeroProfissional()))
            throw new ConflictException("Número profissional já registado.");

        Medico m = Medico.builder()
                .nome(req.nome())
                .especialidade(req.especialidade())
                .contacto(req.contacto())
                .numeroProfissional(req.numeroProfissional())
                .horarioInicio(req.horarioInicio())
                .horarioFim(req.horarioFim())
                .disponivel(req.disponivel())
                .ativo(true)
                .build();

        m = medicoRepository.save(m);
        auditService.registrar(actor, "CRIAR_MEDICO", "Medico", m.getId(), m.getNome());
        return toResponse(m);
    }

    @Transactional
    public MedicoResponse atualizar(UUID id, MedicoRequest req, Utilizador actor) {
        Medico m = findById(id);

        if (!req.numeroProfissional().equals(m.getNumeroProfissional())
                && medicoRepository.existsByNumeroProfissional(req.numeroProfissional()))
            throw new ConflictException("Número profissional já registado.");

        m.setNome(req.nome());
        m.setEspecialidade(req.especialidade());
        m.setContacto(req.contacto());
        m.setNumeroProfissional(req.numeroProfissional());
        m.setHorarioInicio(req.horarioInicio());
        m.setHorarioFim(req.horarioFim());
        m.setDisponivel(req.disponivel());

        auditService.registrar(actor, "ATUALIZAR_MEDICO", "Medico", id, null);
        return toResponse(medicoRepository.save(m));
    }

    @Transactional(readOnly = true)
    public Page<MedicoResponse> listar(String q, Pageable pageable) {
        Page<Medico> page = (q != null && !q.isBlank())
                ? medicoRepository.search(q.trim(), pageable)
                : medicoRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscarPorId(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public MedicoResponse buscarPorUtilizador(UUID utilizadorId) {
        return medicoRepository.findByUtilizadorId(utilizadorId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de médico", utilizadorId));
    }

    @Transactional(readOnly = true)
    public List<MedicoResponse> listarDisponiveis() {
        return medicoRepository.findByDisponivelTrueAndAtivoTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void desativar(UUID id, Utilizador actor) {
        Medico m = findById(id);
        m.setAtivo(false);
        medicoRepository.save(m);
        auditService.registrar(actor, "DESATIVAR_MEDICO", "Medico", id, null);
    }


    private Medico findById(UUID id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", id));
    }

    public MedicoResponse toResponse(Medico m) {
        return new MedicoResponse(
                m.getId(),
                m.getNome(),
                m.getEspecialidade(),
                m.getContacto(),
                m.getNumeroProfissional(),
                m.getHorarioInicio(),
                m.getHorarioFim(),
                m.isDisponivel(),
                m.getFotoUrl(),
                m.isAtivo(),
                m.getCreatedAt()
        );
    }
}