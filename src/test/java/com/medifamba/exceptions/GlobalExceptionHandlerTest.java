package com.medifamba.exceptions;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_deveRetornar404() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException("Paciente", UUID.randomUUID()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
    }

    @Test
    void handleBusiness_deveRetornar422() {
        var response = handler.handleBusiness(new BusinessException("Regra violada"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().message()).isEqualTo("Regra violada");
    }

    @Test
    void handleConflict_deveRetornar409() {
        var response = handler.handleConflict(new ConflictException("Conflito"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("Conflito");
    }

    @Test
    void handleBadCredentials_deveRetornar401() {
        var response = handler.handleBadCredentials(new BadCredentialsException("bad"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Credenciais inválidas");
    }

    @Test
    void handleAccessDenied_deveRetornar403() {
        var response = handler.handleAccessDenied(new AccessDeniedException("denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Acesso negado");
    }

    @Test
    void handleGeneral_deveRetornar500() {
        var response = handler.handleGeneral(new RuntimeException("erro"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Erro interno do servidor");
    }

    @Test
    void handleValidation_deveRetornarCamposInvalidos() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email inválido"));
        var ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().fieldErrors()).containsEntry("email", "Email inválido");
    }
}
