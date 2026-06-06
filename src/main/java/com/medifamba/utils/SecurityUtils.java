package com.medifamba.utils;

import com.medifamba.entities.Utilizador;
import com.medifamba.exceptions.ResourceNotFoundException;
import com.medifamba.repositories.UtilizadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UtilizadorRepository utilizadorRepository;

    public Utilizador getUtilizadorActual() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return utilizadorRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador", email));
    }
}