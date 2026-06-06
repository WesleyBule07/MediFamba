package com.medifamba.services;

import com.medifamba.entities.AuditLog;
import com.medifamba.entities.Utilizador;
import com.medifamba.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void registrar(Utilizador utilizador, String acao,
                          String entidade, UUID entidadeId, String detalhes) {
        auditLogRepository.save(AuditLog.builder()
                .utilizador(utilizador).acao(acao)
                .entidade(entidade).entidadeId(entidadeId)
                .detalhes(detalhes).build());
    }
}