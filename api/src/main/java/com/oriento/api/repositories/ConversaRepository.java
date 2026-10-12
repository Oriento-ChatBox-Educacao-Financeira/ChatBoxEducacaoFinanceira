package com.oriento.api.repositories;

import com.oriento.api.model.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversaRepository extends JpaRepository<Conversa, UUID> {

    List<Conversa> findByIdUsuario(UUID idUsuario);

    List<Conversa> findByEmpresaId(Integer idEmpresa);
}