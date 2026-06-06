package com.medifamba.repositories;

import com.medifamba.entities.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, UUID> {

    @Query("""
        SELECT p FROM Paciente p WHERE p.ativo = true AND (
            LOWER(p.nomeCompleto) LIKE LOWER(CONCAT('%',:q,'%')) OR
            LOWER(p.numeroIdentificacao) LIKE LOWER(CONCAT('%',:q,'%')) OR
            LOWER(p.telefone) LIKE LOWER(CONCAT('%',:q,'%'))
        )
    """)
    Page<Paciente> search(@Param("q") String q, Pageable pageable);

    boolean existsByNumeroIdentificacao(String numeroIdentificacao);

    long countByAtivo(boolean ativo);

    Optional<Paciente> findByUtilizadorId(UUID utilizadorId);
}
