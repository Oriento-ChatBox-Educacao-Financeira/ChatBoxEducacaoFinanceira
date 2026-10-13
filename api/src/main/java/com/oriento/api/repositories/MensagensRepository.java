package com.oriento.api.repositories;

import com.oriento.api.model.Mensagens;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MensagensRepository extends MongoRepository<Mensagens , String> {

    List<Mensagens> findByConversaIdOrderByOrdemAsc(UUID conversaId);

    Optional<Mensagens> findFirstByConversaIdAndRemetenteOrderByOrdemAsc(UUID conversaId, String remetente);

    Optional<Mensagens> findFirstByConversaIdOrderByOrdemDesc(UUID conversaId);

    long countByConversaId(UUID conversaId);
}
