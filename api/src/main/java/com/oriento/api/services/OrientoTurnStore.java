package com.oriento.api.services;

import com.oriento.api.dto.ai.HistoryMessage;
import com.oriento.api.model.OrientoTurn;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrientoTurnStore {
    private final MongoTemplate mongoTemplate;

    public OrientoTurnStore(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public HistorySnapshot load(UUID conversationId) {
        try {
            Query query = Query.query(Criteria.where("conversationId").is(conversationId.toString()))
                    .with(Sort.by(Sort.Direction.DESC, "ordem")).limit(20);
            List<OrientoTurn> turns = new ArrayList<>(mongoTemplate.find(query, OrientoTurn.class));
            long nextOrder = turns.isEmpty() ? 1 : turns.getFirst().ordem() + 1;
            List<HistoryMessage> history = new ArrayList<>();
            for (OrientoTurn turn : turns.reversed()) {
                history.add(new HistoryMessage("user", turn.pergunta()));
                history.add(new HistoryMessage("assistant", turn.resposta()));
            }
            return new HistorySnapshot(nextOrder, List.copyOf(history));
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Histórico indisponível");
        }
    }

    public void append(UUID conversationId, long order, String question, String response) {
        try {
            mongoTemplate.insert(new OrientoTurn(null, conversationId.toString(), order,
                    question, response, Instant.now()));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A conversa recebeu outra mensagem durante a geração. Tente novamente.");
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível salvar o histórico");
        }
    }

    public record HistorySnapshot(long nextOrder, List<HistoryMessage> messages) {}
}
