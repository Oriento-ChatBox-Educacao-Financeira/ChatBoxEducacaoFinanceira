package com.oriento.api.services;

import com.oriento.api.client.OrientoAiClient;
import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.ai.OrientoAskRequest;
import com.oriento.api.model.AIConversation;
import com.oriento.api.model.Empresa;
import com.oriento.api.model.Usuario;
import com.oriento.api.model.enuns.StatusConversa;
import com.oriento.api.repositories.AIConversationRepository;
import com.oriento.api.repositories.EmpresaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final OrientoAiClient aiClient;
    private final AIConversationRepository conversations;
    private final EmpresaRepository empresas;
    private final OrientoTurnStore turns;
    private final OrientoAiProperties properties;

    public AIService(OrientoAiClient aiClient, AIConversationRepository conversations,
                     EmpresaRepository empresas, OrientoTurnStore turns, OrientoAiProperties properties) {
        this.aiClient = aiClient;
        this.conversations = conversations;
        this.empresas = empresas;
        this.turns = turns;
        this.properties = properties;
    }

    public AskResponse askOriento(String prompt, String conversationId, Usuario usuario, String userToken) {
        if (!properties.enabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Assistente temporariamente desabilitado");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("A pergunta não pode ser vazia");
        }

        long started = System.nanoTime();
        try {
            UUID requestedId = parseConversationId(conversationId);
            AIConversation conversation = requestedId == null ? null : conversations.findById(requestedId)
                    .orElseThrow(() -> new EntityNotFoundException("Conversa não encontrada"));
            if (conversation != null && !conversation.pertenceAo(usuario)) {
                throw new AccessDeniedException("Conversa não pertence ao usuário autenticado");
            }
            if (conversation != null && conversation.getStatus() == StatusConversa.encerrada) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Conversa encerrada");
            }

            Empresa empresa = empresas.findByUsuarioIdUsuario(usuario.getIdUsuario()).orElse(null);
            if (conversation != null && conversation.getEmpresa() != null
                    && (empresa == null || !conversation.getEmpresa().getId().equals(empresa.getId()))) {
                throw new AccessDeniedException("Empresa da conversa não pertence ao usuário autenticado");
            }
            if (conversation == null) {
                conversation = new AIConversation(UUID.randomUUID(), usuario);
                conversation.setEmpresa(empresa);
                conversation.setStatus(StatusConversa.ativa);
                conversation.setIniciadaEm(OffsetDateTime.now());
                conversation = conversations.save(conversation);
            }

            UUID effectiveId = conversation.getIdConversa();
            OrientoTurnStore.HistorySnapshot snapshot = turns.load(effectiveId);
            AskResponse response = aiClient.ask(new OrientoAskRequest(
                    effectiveId, prompt, empresa == null ? null : empresa.getId(), snapshot.messages()), userToken);
            turns.append(effectiveId, snapshot.nextOrder(), prompt, response.response());
            return response;
        } finally {
            logger.info("Solicitação de IA concluída durationMs={}", (System.nanoTime() - started) / 1_000_000);
        }
    }

    private UUID parseConversationId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            UUID id = UUID.fromString(value);
            if (!id.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }
            return id;
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Identificador de conversa inválido");
        }
    }
}
