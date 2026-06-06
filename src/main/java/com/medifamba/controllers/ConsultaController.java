package com.medifamba.controllers;

import com.medifamba.dto.request.*;
import com.medifamba.dto.response.ConsultaResponse;
import com.medifamba.enums.ConsultaEstado;
import com.medifamba.services.ConsultaService;
import com.medifamba.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/consultas")
@RequiredArgsConstructor
public class ConsultaController {

    private final ConsultaService consultaService;
    private final SecurityUtils securityUtils;

    @GetMapping("/hoje")
    public ResponseEntity<List<ConsultaResponse>> hoje(
            @RequestParam(required = false) ConsultaEstado estado) {
        return ResponseEntity.ok(consultaService.listarHoje(estado));
    }

    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<List<LocalDateTime>> horariosDisponiveis(
            @RequestParam UUID medicoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(consultaService.horariosDisponiveis(medicoId, data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultaResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(consultaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<ConsultaResponse> agendar(@Valid @RequestBody ConsultaRequest req) {
        return ResponseEntity.status(201)
                .body(consultaService.agendar(req, securityUtils.getUtilizadorActual()));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ConsultaResponse> alterarEstado(
            @PathVariable UUID id, @Valid @RequestBody AlterarEstadoRequest req) {
        return ResponseEntity.ok(
                consultaService.alterarEstado(id, req, securityUtils.getUtilizadorActual()));
    }

    @PatchMapping("/{id}/remarcar")
    public ResponseEntity<ConsultaResponse> remarcar(
            @PathVariable UUID id, @Valid @RequestBody RemarcarRequest req) {
        return ResponseEntity.ok(
                consultaService.remarcar(id, req, securityUtils.getUtilizadorActual()));
    }
}