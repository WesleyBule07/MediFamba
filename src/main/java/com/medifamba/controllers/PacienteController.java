package com.medifamba.controllers;

import com.medifamba.dto.request.PacienteRequest;
import com.medifamba.dto.response.ConsultaResponse;
import com.medifamba.dto.response.PageResponse;
import com.medifamba.dto.response.PacienteResponse;
import com.medifamba.services.ConsultaService;
import com.medifamba.services.PacienteService;
import com.medifamba.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;
    private final ConsultaService consultaService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<PageResponse<PacienteResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        var pageable = PageRequest.of(page, size, Sort.by("nomeCompleto"));
        return ResponseEntity.ok(
                PageResponse.of(pacienteService.listar(q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> criar(
            @Valid @RequestBody PacienteRequest req) {
        return ResponseEntity.status(201)
                .body(pacienteService.criar(req, securityUtils.getUtilizadorActual()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponse> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody PacienteRequest req) {
        return ResponseEntity.ok(
                pacienteService.atualizar(id, req, securityUtils.getUtilizadorActual()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        pacienteService.desativar(id, securityUtils.getUtilizadorActual());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/meu-perfil")
    public ResponseEntity<PacienteResponse> meuPerfil() {
        var actor = securityUtils.getUtilizadorActual();
        return ResponseEntity.ok(
                pacienteService.buscarPorUtilizador(actor.getId()));
    }

    @GetMapping("/minhas-consultas")
    public ResponseEntity<PageResponse<ConsultaResponse>> minhasConsultas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var actor = securityUtils.getUtilizadorActual();
        var perfil = pacienteService.buscarPorUtilizador(actor.getId());
        var pageable = PageRequest.of(page, size,
                Sort.by("dataHora").descending());
        return ResponseEntity.ok(
                PageResponse.of(
                        consultaService.listarPorPaciente(perfil.id(), pageable)));
    }
}