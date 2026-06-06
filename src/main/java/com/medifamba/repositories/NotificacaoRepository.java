package com.medifamba.repositories;

import com.medifamba.entities.Notificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {
    Page<Notificacao> findByUtilizadorIdOrderByCreatedAtDesc(UUID utilizadorId, Pageable pageable);
    long countByUtilizadorIdAndLidaFalse(UUID utilizadorId);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.utilizador.id = :uid")
    void marcarTodasComoLidas(@Param("uid") UUID utilizadorId);
}