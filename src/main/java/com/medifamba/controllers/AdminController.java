package com.medifamba.controllers;

import com.medifamba.dto.request.CriarUtilizadorComPerfilRequest;
import com.medifamba.dto.response.DashboardResponse;
import com.medifamba.dto.response.PageResponse;
import com.medifamba.dto.response.UtilizadorResponse;
import com.medifamba.services.DashboardService;
import com.medifamba.services.UtilizadorService;
import com.medifamba.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DashboardService dashboardService;
    private final UtilizadorService utilizadorService;
    private final SecurityUtils securityUtils;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(
                dashboardService.getDashboard(securityUtils.getUtilizadorActual()));
    }

    @GetMapping("/utilizadores")
    public ResponseEntity<PageResponse<UtilizadorResponse>> listarUtilizadores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(
                PageResponse.of(utilizadorService.listar(pageable)));
    }

    @PostMapping("/utilizadores")
    public ResponseEntity<UtilizadorResponse> criarUtilizador(
            @Valid @RequestBody CriarUtilizadorComPerfilRequest req) {
        return ResponseEntity.status(201).body(
                utilizadorService.criarComPerfil(req, securityUtils.getUtilizadorActual()));
    }

    @PatchMapping("/utilizadores/{id}/ativar")
    public ResponseEntity<UtilizadorResponse> ativar(@PathVariable UUID id) {
        return ResponseEntity.ok(
                utilizadorService.alterarEstado(id, true,
                        securityUtils.getUtilizadorActual()));
    }

    @PatchMapping("/utilizadores/{id}/desativar")
    public ResponseEntity<UtilizadorResponse> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(
                utilizadorService.alterarEstado(id, false,
                        securityUtils.getUtilizadorActual()));
    }
}