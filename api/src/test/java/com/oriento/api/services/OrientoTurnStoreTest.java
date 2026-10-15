package com.oriento.api.services;

import com.oriento.api.model.OrientoTurn;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrientoTurnStoreTest {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final OrientoTurnStore store = new OrientoTurnStore(mongo);

    @Test
    void failedHistoryReadIs503() {
        when(mongo.find(any(Query.class), eq(OrientoTurn.class)))
                .thenThrow(new DataAccessResourceFailureException("sensitive storage error"));
        assertThatThrownBy(() -> store.load(UUID.randomUUID())).isInstanceOfSatisfying(
                ResponseStatusException.class, error -> {
                    assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(error.getReason()).doesNotContain("sensitive");
                });
    }

    @Test
    void duplicateTurnIs409() {
        when(mongo.insert(any(OrientoTurn.class))).thenThrow(new DuplicateKeyException("duplicate"));
        assertThatThrownBy(() -> store.append(UUID.randomUUID(), 1, "Pergunta", "Resposta"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void failedHistoryWriteIs503() {
        when(mongo.insert(any(OrientoTurn.class))).thenThrow(new DataAccessResourceFailureException("failure"));
        assertThatThrownBy(() -> store.append(UUID.randomUUID(), 1, "Pergunta", "Resposta"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }
}
