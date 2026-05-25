package com.oriento.api.repositories;

import com.oriento.api.model.AIConversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, String> {
}

