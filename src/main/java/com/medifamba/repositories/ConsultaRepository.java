package com.medifamba.repositories;

import com.medifamba.entities.Consulta;
import com.medifamba.enums.ConsultaEstado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {

    @Query("""
        SELECT c FROM Consulta c
        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        WHERE c.dataHora >= :inicio AND c.dataHora < :fim
        ORDER BY c.dataHora ASC
    """)
    List<Consulta> findByDia(@Param("inicio") LocalDateTime inicio,
                             @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT c FROM Consulta c
        JOIN FETCH c.paciente
        JOIN FETCH c.medico
        WHERE c.dataHora >= :inicio AND c.dataHora < :fim
        AND c.estado = :estado
        ORDER BY c.dataHora ASC
    """)
    List<Consulta> findByDiaAndEstado(@Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim,
                                      @Param("estado") ConsultaEstado estado);

    @Query("""
        SELECT c FROM Consulta c
        WHERE c.medico.id = :medicoId
        AND c.dataHora >= :inicio AND c.dataHora < :fim
        AND c.estado NOT IN ('CANCELADA','CONCLUIDA')
        ORDER BY c.dataHora ASC
    """)
    List<Consulta> findByMedicoAndDia(@Param("medicoId") UUID medicoId,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fim") LocalDateTime fim);

    Page<Consulta> findByPacienteIdOrderByDataHoraDesc(UUID pacienteId, Pageable pageable);

    @Query("""
        SELECT COUNT(c) > 0 FROM Consulta c
        WHERE c.medico.id = :medicoId
        AND c.dataHora = :dataHora
        AND c.estado NOT IN ('CANCELADA','CONCLUIDA')
        AND (:excludeId IS NULL OR c.id != :excludeId)
    """)
    boolean existeConflito(@Param("medicoId") UUID medicoId,
                           @Param("dataHora") LocalDateTime dataHora,
                           @Param("excludeId") UUID excludeId);

    long countByEstado(ConsultaEstado estado);

    @Query("SELECT COUNT(c) FROM Consulta c WHERE c.dataHora >= :inicio AND c.dataHora < :fim")
    long countByDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}