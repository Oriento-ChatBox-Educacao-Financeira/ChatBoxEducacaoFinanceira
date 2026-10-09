package com.oriento.api.repositories;

import com.oriento.api.model.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByIdUsuarioOrderByDataCriacaoDesc(UUID idUsuario, Pageable pageable);

    @Modifying
    @Query("""
            update Notificacao n
               set n.lida = true,
                   n.dataLeitura = :agora
             where n.idUsuario = :idUsuario
               and n.lida = false
            """)
    int marcarTodasLidas(@Param("idUsuario") UUID idUsuario,
                         @Param("agora") OffsetDateTime agora);
}
