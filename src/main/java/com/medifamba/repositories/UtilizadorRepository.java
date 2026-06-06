package com.medifamba.repositories;

import com.medifamba.entities.Utilizador;
import com.medifamba.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, UUID> {
    Optional<Utilizador> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
    long countByAtivo(boolean ativo);
}