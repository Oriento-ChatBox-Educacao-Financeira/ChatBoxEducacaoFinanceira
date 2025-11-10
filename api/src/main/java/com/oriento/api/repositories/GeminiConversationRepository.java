package com.oriento.api.repositories;

import com.oriento.api.model.GeminiConversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GeminiConversationRepository extends JpaRepository<GeminiConversation, String> {

    Optional<GeminiConversation> findByConversationId(String conversationId);
}

