package com.medifamba.dto.request;

import com.medifamba.enums.Genero;
import com.medifamba.enums.UserRole;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

public record CriarUtilizadorComPerfilRequest(
        // Dados da conta
        @NotBlank @Size(min=2,max=150) String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min=8) String senha,
        @NotNull UserRole role,

        // Dados do perfil — médico
        String especialidade,
        String numeroProfissional,
        String contacto,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        Boolean disponivel,

        // Dados do perfil — paciente
        LocalDate dataNascimento,
        Genero genero,
        String telefone,
        String endereco,
        String contactoEmergencia,
        String numeroIdentificacao,
        String historicoBasico
) {}