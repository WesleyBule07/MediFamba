package com.medifamba.services;

import com.medifamba.dto.request.*;
import com.medifamba.dto.response.ConsultaResponse;
import com.medifamba.entities.*;
import com.medifamba.enums.ConsultaEstado;
import com.medifamba.enums.NotificacaoTipo;
import com.medifamba.exceptions.*;
import com.medifamba.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AuditService auditService;

    @Transactional
    public ConsultaResponse agendar(ConsultaRequest req, Utilizador actor) {
        Paciente paciente = pacienteRepository.findById(req.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", req.pacienteId()));
        Medico medico = medicoRepository.findById(req.medicoId())
                .orElseThrow(() -> new ResourceNotFoundException("Médico", req.medicoId()));

        var hora = req.dataHora().toLocalTime();
        if (hora.isBefore(medico.getHorarioInicio()) || hora.isAfter(medico.getHorarioFim()))
            throw new BusinessException("Horário fora do período de atendimento do médico.");

        if (consultaRepository.existeConflito(medico.getId(), req.dataHora(), null))
            throw new ConflictException("Médico já tem consulta neste horário.");

        Consulta c = Consulta.builder()
                .paciente(paciente).medico(medico)
                .dataHora(req.dataHora()).motivo(req.motivo())
                .sala(req.sala()).estado(ConsultaEstado.PENDENTE)
                .criadoPor(actor).build();

        c = consultaRepository.save(c);
        notificar(medico.getUtilizador(), NotificacaoTipo.NOVA_CONSULTA,
                "Nova consulta agendada",
                "Paciente " + paciente.getNomeCompleto() + " em " + req.dataHora(), c);
        auditService.registrar(actor, "AGENDAR_CONSULTA", "Consulta", c.getId(), null);
        return toResponse(c);
    }

    @Transactional
    public ConsultaResponse alterarEstado(UUID id, AlterarEstadoRequest req, Utilizador actor) {
        Consulta c = findById(id);
        c.setEstado(req.estado());
        if (req.observacoes() != null) c.setObservacoes(req.observacoes());
        c = consultaRepository.save(c);

        if (c.getPaciente().getUtilizador() != null) {
            String msg = switch (req.estado()) {
                case CONFIRMADA -> "A sua consulta foi confirmada.";
                case CANCELADA  -> "A sua consulta foi cancelada.";
                case REMARCADA  -> "A sua consulta foi remarcada.";
                default         -> "Estado da consulta atualizado.";
            };
            notificar(c.getPaciente().getUtilizador(),
                    req.estado() == ConsultaEstado.CANCELADA
                            ? NotificacaoTipo.CANCELAMENTO : NotificacaoTipo.ATUALIZACAO,
                    "Consulta atualizada", msg, c);
        }
        auditService.registrar(actor, "ALTERAR_ESTADO", "Consulta", id, req.estado().name());
        return toResponse(c);
    }

    @Transactional
    public ConsultaResponse remarcar(UUID id, RemarcarRequest req, Utilizador actor) {
        Consulta c = findById(id);
        if (consultaRepository.existeConflito(c.getMedico().getId(), req.novaDataHora(), id))
            throw new ConflictException("Médico já tem consulta neste horário.");
        c.setDataHora(req.novaDataHora());
        c.setEstado(ConsultaEstado.REMARCADA);
        if (req.motivo() != null) c.setObservacoes(req.motivo());
        c = consultaRepository.save(c);
        if (c.getPaciente().getUtilizador() != null)
            notificar(c.getPaciente().getUtilizador(), NotificacaoTipo.ALTERACAO_HORARIO,
                    "Consulta remarcada", "Nova data: " + req.novaDataHora(), c);
        auditService.registrar(actor, "REMARCAR_CONSULTA", "Consulta", id, null);
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> listarHoje(ConsultaEstado estado) {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim = inicio.plusDays(1);
        List<Consulta> lista = estado != null
                ? consultaRepository.findByDiaAndEstado(inicio, fim, estado)
                : consultaRepository.findByDia(inicio, fim);
        return lista.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ConsultaResponse> listarPorPaciente(UUID pacienteId, Pageable pageable) {
        return consultaRepository.findByPacienteIdOrderByDataHoraDesc(pacienteId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ConsultaResponse buscarPorId(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public List<LocalDateTime> horariosDisponiveis(UUID medicoId, LocalDate data) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", medicoId));
        List<Consulta> ocupadas = consultaRepository.findByMedicoAndDia(
                medicoId, data.atStartOfDay(), data.plusDays(1).atStartOfDay());
        List<LocalDateTime> ocupadasHoras = ocupadas.stream().map(Consulta::getDataHora).toList();
        List<LocalDateTime> disponiveis = new ArrayList<>();
        LocalDateTime slot = data.atTime(medico.getHorarioInicio());
        LocalDateTime fim = data.atTime(medico.getHorarioFim());
        while (!slot.isAfter(fim.minusMinutes(30))) {
            if (!ocupadasHoras.contains(slot)) disponiveis.add(slot);
            slot = slot.plusMinutes(30);
        }
        return disponiveis;
    }

    private Consulta findById(UUID id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta", id));
    }

    private void notificar(Utilizador u, NotificacaoTipo tipo,
                           String titulo, String mensagem, Consulta c) {
        if (u == null) return;
        notificacaoRepository.save(Notificacao.builder()
                .utilizador(u).tipo(tipo).titulo(titulo).mensagem(mensagem).consulta(c).build());
    }

    public ConsultaResponse toResponse(Consulta c) {
        return new ConsultaResponse(
                c.getId(),
                new ConsultaResponse.PacienteSummary(c.getPaciente().getId(),
                        c.getPaciente().getNomeCompleto(),
                        c.getPaciente().getTelefone(),
                        c.getPaciente().getFotoUrl()),
                new ConsultaResponse.MedicoSummary(c.getMedico().getId(),
                        c.getMedico().getNome(),
                        c.getMedico().getEspecialidade(),
                        c.getMedico().isDisponivel(),
                        c.getMedico().getFotoUrl()),
                c.getDataHora(), c.getMotivo(), c.getSala(),
                c.getEstado(), c.getObservacoes(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ConsultaResponse> listarHojeParaMedico(UUID utilizadorId) {
        // Busca o médico pelo utilizadorId
        Medico medico = medicoRepository.findByUtilizadorId(utilizadorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Perfil de médico", utilizadorId));
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim = inicio.plusDays(1);
        return consultaRepository.findByMedicoAndDia(medico.getId(), inicio, fim)
                .stream().map(this::toResponse).toList();
    }
}