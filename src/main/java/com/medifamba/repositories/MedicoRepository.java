package com.medifamba.repositories;

import com.medifamba.entities.Medico;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, UUID> {

    @Query("""
        SELECT m FROM Medico m WHERE m.ativo = true AND (
            LOWER(m.nome) LIKE LOWER(CONCAT('%',:q,'%')) OR
            LOWER(m.especialidade) LIKE LOWER(CONCAT('%',:q,'%'))
        )
    """)
    Page<Medico> search(@Param("q") String q, Pageable pageable);

    List<Medico> findByDisponivelTrueAndAtivoTrue();
    boolean existsByNumeroProfissional(String numeroProfissional);
    long countByAtivo(boolean ativo);
    long countByDisponivelTrue();


    Optional<Medico> findByUtilizadorId(UUID utilizadorId);
}