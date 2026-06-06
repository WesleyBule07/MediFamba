package com.medifamba.controllers;

import com.medifamba.dto.response.NotificacaoResponse;
import com.medifamba.dto.response.PageResponse;
import com.medifamba.services.NotificacaoService;
import com.medifamba.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<PageResponse<NotificacaoResponse>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var utilizador = securityUtils.getUtilizadorActual();
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                PageResponse.of(notificacaoService.listar(utilizador.getId(), pageable)));
    }

    @PatchMapping("/marcar-lidas")
    public ResponseEntity<Void> marcarLidas() {
        notificacaoService.marcarTodasComoLidas(
                securityUtils.getUtilizadorActual().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/nao-lidas/count")
    public ResponseEntity<Long> contarNaoLidas() {
        return ResponseEntity.ok(notificacaoService.contarNaoLidas(
                securityUtils.getUtilizadorActual().getId()));
    }
}