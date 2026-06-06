package com.medifamba.services;

import com.medifamba.dto.response.NotificacaoResponse;
import com.medifamba.repositories.NotificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    @Transactional(readOnly = true)
    public Page<NotificacaoResponse> listar(UUID utilizadorId, Pageable pageable) {
        return notificacaoRepository
                .findByUtilizadorIdOrderByCreatedAtDesc(utilizadorId, pageable)
                .map(n -> new NotificacaoResponse(n.getId(), n.getTipo(),
                        n.getTitulo(), n.getMensagem(), n.isLida(),
                        n.getConsulta() != null ? n.getConsulta().getId() : null,
                        n.getCreatedAt()));
    }

    @Transactional
    public void marcarTodasComoLidas(UUID utilizadorId) {
        notificacaoRepository.marcarTodasComoLidas(utilizadorId);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(UUID utilizadorId) {
        return notificacaoRepository.countByUtilizadorIdAndLidaFalse(utilizadorId);
    }
}