package com.medifamba.services;

import com.medifamba.entities.AuditLog;
import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import com.medifamba.repositories.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditService auditService;

    @Test
    void registrar_deveGuardarRegistoDeAuditoria() {
        Utilizador utilizador = Utilizador.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .email("admin@medifamba.com")
                .senha("hash")
                .role(UserRole.ADMIN)
                .ativo(true)
                .build();
        UUID entidadeId = UUID.randomUUID();

        auditService.registrar(utilizador, "CRIAR", "Consulta", entidadeId, "detalhes");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getUtilizador()).isEqualTo(utilizador);
        assertThat(log.getAcao()).isEqualTo("CRIAR");
        assertThat(log.getEntidade()).isEqualTo("Consulta");
        assertThat(log.getEntidadeId()).isEqualTo(entidadeId);
        assertThat(log.getDetalhes()).isEqualTo("detalhes");
    }
}
