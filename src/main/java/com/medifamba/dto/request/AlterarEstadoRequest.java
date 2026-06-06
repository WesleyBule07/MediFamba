package com.medifamba.dto.request;
import com.medifamba.enums.ConsultaEstado;
import jakarta.validation.constraints.NotNull;

public record AlterarEstadoRequest(
        @NotNull ConsultaEstado estado,
        String observacoes
) {}