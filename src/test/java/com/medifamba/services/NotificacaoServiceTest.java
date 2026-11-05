package com.medifamba.services;

import com.medifamba.dto.response.NotificacaoResponse;
import com.medifamba.entities.Consulta;
import com.medifamba.entities.Notificacao;
import com.medifamba.enums.NotificacaoTipo;
import com.medifamba.repositories.NotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock private NotificacaoRepository notificacaoRepository;

    @InjectMocks private NotificacaoService notificacaoService;

    @Test
    void listar_deveMapearNotificacoesIncluindoConsulta() {
        UUID utilizadorId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Consulta consulta = Consulta.builder().id(consultaId).build();
        Notificacao notificacao = Notificacao.builder()
                .id(UUID.randomUUID())
                .tipo(NotificacaoTipo.NOVA_CONSULTA)
                .titulo("Nova consulta")
                .mensagem("Agendada")
                .lida(false)
                .consulta(consulta)
                .build();
        when(notificacaoRepository.findByUtilizadorIdOrderByCreatedAtDesc(utilizadorId, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacao), pageable, 1));

        Page<NotificacaoResponse> result = notificacaoService.listar(utilizadorId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).consultaId()).isEqualTo(consultaId);
        assertThat(result.getContent().get(0).tipo()).isEqualTo(NotificacaoTipo.NOVA_CONSULTA);
        assertThat(result.getContent().get(0).lida()).isFalse();
    }

    @Test
    void listar_deveRetornarConsultaIdNuloQuandoSemConsulta() {
        UUID utilizadorId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Notificacao notificacao = Notificacao.builder()
                .id(UUID.randomUUID())
                .tipo(NotificacaoTipo.ATUALIZACAO)
                .titulo("Atualizacao")
                .mensagem("Mensagem")
                .lida(true)
                .build();
        when(notificacaoRepository.findByUtilizadorIdOrderByCreatedAtDesc(utilizadorId, pageable))
                .thenReturn(new PageImpl<>(List.of(notificacao), pageable, 1));

        Page<NotificacaoResponse> result = notificacaoService.listar(utilizadorId, pageable);

        assertThat(result.getContent().get(0).consultaId()).isNull();
    }

    @Test
    void marcarTodasComoLidas_deveDelegarNoRepositorio() {
        UUID utilizadorId = UUID.randomUUID();

        notificacaoService.marcarTodasComoLidas(utilizadorId);

        verify(notificacaoRepository).marcarTodasComoLidas(utilizadorId);
    }

    @Test
    void contarNaoLidas_deveRetornarContagem() {
        UUID utilizadorId = UUID.randomUUID();
        when(notificacaoRepository.countByUtilizadorIdAndLidaFalse(utilizadorId)).thenReturn(7L);

        long result = notificacaoService.contarNaoLidas(utilizadorId);

        assertThat(result).isEqualTo(7L);
    }
}
