package com.oriento.api.repositories;

import com.oriento.api.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    /** Listagem filtrada pela empresa do token — substitui findAll() */
    List<Conversa> findByEmpresaId(Integer idEmpresa);

    /**
     * Busca individual com verificação de posse (anti-IDOR).
     * Retorna vazio se o id existir mas não pertencer à empresa autenticada.
     */
    Optional<Conversa> findByIdConversaAndEmpresaId(UUID idConversa, Integer idEmpresa);
}