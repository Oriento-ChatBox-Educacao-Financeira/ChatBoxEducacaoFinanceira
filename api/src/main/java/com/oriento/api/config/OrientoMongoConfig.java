package com.oriento.api.config;

import com.oriento.api.model.OrientoTurn;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
@ConditionalOnProperty(name = "oriento.ai.enabled", havingValue = "true")
public class OrientoMongoConfig {
    @Bean
    public InitializingBean orientoTurnIndex(MongoTemplate mongoTemplate) {
        return () -> mongoTemplate.indexOps(OrientoTurn.class).ensureIndex(
                new Index().on("conversationId", Sort.Direction.ASC).on("ordem", Sort.Direction.ASC)
                        .unique().named("uk_oriento_conversa_ordem"));
    }
}
