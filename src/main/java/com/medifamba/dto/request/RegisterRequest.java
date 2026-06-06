package com.medifamba.dto.request;
import com.medifamba.enums.UserRole;
import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min=2,max=150) String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min=8) String senha,
        UserRole role
) {}