package com.medifamba.controllers;

import com.medifamba.dto.request.MedicoRequest;
import com.medifamba.dto.response.ConsultaResponse;
import com.medifamba.dto.response.MedicoResponse;
import com.medifamba.dto.response.PageResponse;
import com.medifamba.services.ConsultaService;
import com.medifamba.services.MedicoService;
import com.medifamba.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicoService medicoService;
    private final ConsultaService consultaService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<PageResponse<MedicoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("nome"));
        return ResponseEntity.ok(PageResponse.of(medicoService.listar(q, pageable)));
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<MedicoResponse>> disponiveis() {
        return ResponseEntity.ok(medicoService.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(medicoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<MedicoResponse> criar(
            @Valid @RequestBody MedicoRequest req) {
        return ResponseEntity.status(201)
                .body(medicoService.criar(req, securityUtils.getUtilizadorActual()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicoResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody MedicoRequest req) {
        return ResponseEntity.ok(
                medicoService.atualizar(id, req, securityUtils.getUtilizadorActual()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        medicoService.desativar(id, securityUtils.getUtilizadorActual());
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/meu-painel")
    public ResponseEntity<MedicoResponse> meuPainel() {
        var actor = securityUtils.getUtilizadorActual();
        return ResponseEntity.ok(
                medicoService.buscarPorUtilizador(actor.getId()));
    }

    @GetMapping("/minhas-consultas-hoje")
    public ResponseEntity<List<ConsultaResponse>> minhasConsultasHoje() {
        var actor = securityUtils.getUtilizadorActual();
        return ResponseEntity.ok(
                consultaService.listarHojeParaMedico(actor.getId()));
    }
}