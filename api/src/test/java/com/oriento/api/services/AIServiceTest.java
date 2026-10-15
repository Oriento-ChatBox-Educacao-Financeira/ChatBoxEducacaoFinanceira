package com.oriento.api.services;

import com.oriento.api.client.OrientoAiClient;
import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.ai.HistoryMessage;
import com.oriento.api.dto.ai.OrientoAskRequest;
import com.oriento.api.model.*;
import com.oriento.api.model.enuns.StatusConversa;
import com.oriento.api.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AIServiceTest {
    final OrientoAiClient client = mock(OrientoAiClient.class);
    final AIConversationRepository conversations = mock(AIConversationRepository.class);
    final EmpresaRepository empresas = mock(EmpresaRepository.class);
    final OrientoTurnStore turns = mock(OrientoTurnStore.class);
    Usuario user;
    AIService service;

    static OrientoAiProperties properties(boolean enabled) {
        return new OrientoAiProperties(enabled, URI.create("http://localhost:8000"),
                Duration.ofSeconds(5), Duration.ofSeconds(150), "test-secret");
    }

    @BeforeEach
    void setup() {
        user = new Usuario();
        user.setIdUsuario(UUID.randomUUID());
        service = new AIService(client, conversations, empresas, turns, properties(true));
        when(empresas.findByUsuarioIdUsuario(user.getIdUsuario())).thenReturn(Optional.empty());
    }

    @Test
    void flagOffDoesNotCreateConversationOrCallDependencies() {
        service = new AIService(client, conversations, empresas, turns, properties(false));
        assertThatThrownBy(() -> service.askOriento("Pergunta", null, user, "jwt"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
        verifyNoInteractions(client, conversations, empresas, turns);
    }

    @Test
    void firstQuestionLinksUserCompanyAndInitializesConversation() {
        Empresa company = new Empresa(7);
        company.setUsuario(user);
        when(empresas.findByUsuarioIdUsuario(user.getIdUsuario())).thenReturn(Optional.of(company));
        when(conversations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(turns.load(any())).thenReturn(new OrientoTurnStore.HistorySnapshot(1, List.of()));
        when(client.ask(any(), eq("jwt"))).thenAnswer(invocation -> {
            OrientoAskRequest request = invocation.getArgument(0);
            return new AskResponse(request.conversationId().toString(), "Resposta");
        });
        AskResponse result = service.askOriento("Pergunta", null, user, "jwt");
        ArgumentCaptor<AIConversation> created = ArgumentCaptor.forClass(AIConversation.class);
        verify(conversations).save(created.capture());
        assertThat(created.getValue().getUsuario()).isSameAs(user);
        assertThat(created.getValue().getEmpresa()).isSameAs(company);
        assertThat(created.getValue().getStatus()).isEqualTo(StatusConversa.ativa);
        assertThat(created.getValue().getIniciadaEm()).isNotNull();
        ArgumentCaptor<OrientoAskRequest> sent = ArgumentCaptor.forClass(OrientoAskRequest.class);
        verify(client).ask(sent.capture(), eq("jwt"));
        assertThat(sent.getValue().empresaId()).isEqualTo(7);
        assertThat(sent.getValue().history()).isEmpty();
        verify(turns).append(UUID.fromString(result.conversationId()), 1, "Pergunta", "Resposta");
    }

    @Test
    void reconstructedServiceLoadsPersistedHistoryAndKeepsConversationId() {
        AIConversation conversation = existing(user);
        List<HistoryMessage> history = List.of(new HistoryMessage("user", "Anterior"),
                new HistoryMessage("assistant", "Resposta anterior"));
        when(turns.load(conversation.getIdConversa()))
                .thenReturn(new OrientoTurnStore.HistorySnapshot(2, history));
        when(client.ask(any(), anyString())).thenReturn(
                new AskResponse(conversation.getIdConversa().toString(), "Nova resposta"));
        service = new AIService(client, conversations, empresas, turns, properties(true));
        AskResponse result = service.askOriento("Nova pergunta", conversation.getIdConversa().toString(), user, "jwt");
        ArgumentCaptor<OrientoAskRequest> sent = ArgumentCaptor.forClass(OrientoAskRequest.class);
        verify(client).ask(sent.capture(), eq("jwt"));
        assertThat(sent.getValue().history()).isEqualTo(history);
        assertThat(result.conversationId()).isEqualTo(conversation.getIdConversa().toString());
        verify(conversations, never()).save(any());
        verify(turns).append(conversation.getIdConversa(), 2, "Nova pergunta", "Nova resposta");
    }

    @Test
    void noCompanySendsGenericRequest() {
        AIConversation conversation = existing(user);
        when(turns.load(any())).thenReturn(new OrientoTurnStore.HistorySnapshot(1, List.of()));
        when(client.ask(any(), anyString())).thenReturn(new AskResponse(conversation.getIdConversa().toString(), "Geral"));
        service.askOriento("Pergunta", conversation.getIdConversa().toString(), user, "jwt");
        verify(client).ask(argThat(request -> request.empresaId() == null), eq("jwt"));
    }

    @Test
    void invalidInputDoesNotCreateConversation() {
        assertThatThrownBy(() -> service.askOriento(" ", null, user, "jwt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.askOriento("Pergunta", "1-1-1-1-1", user, "jwt"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(conversations, client, turns);
    }

    @Test
    void missingConversationIs404() {
        UUID id = UUID.randomUUID();
        when(conversations.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.askOriento("Pergunta", id.toString(), user, "jwt"))
                .isInstanceOf(EntityNotFoundException.class);
        verifyNoInteractions(client, turns);
    }

    @Test
    void otherUsersConversationIsForbidden() {
        Usuario other = new Usuario();
        other.setIdUsuario(UUID.randomUUID());
        AIConversation conversation = existing(other);
        assertThatThrownBy(() -> service.askOriento("Pergunta", conversation.getIdConversa().toString(), user, "jwt"))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(client, turns);
    }

    @Test
    void closedConversationIsConflict() {
        AIConversation conversation = existing(user);
        conversation.setStatus(StatusConversa.encerrada);
        assertThatThrownBy(() -> service.askOriento("Pergunta", conversation.getIdConversa().toString(), user, "jwt"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(client, turns);
    }

    @Test
    void failedGenerationDoesNotPersistTurn() {
        AIConversation conversation = existing(user);
        when(turns.load(any())).thenReturn(new OrientoTurnStore.HistorySnapshot(1, List.of()));
        when(client.ask(any(), anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> service.askOriento("Pergunta", conversation.getIdConversa().toString(), user, "jwt"))
                .isInstanceOf(ResponseStatusException.class);
        verify(turns, never()).append(any(), anyLong(), anyString(), anyString());
    }

    @Test
    void persistenceFailurePreventsSuccessfulResponse() {
        AIConversation conversation = existing(user);
        when(turns.load(any())).thenReturn(new OrientoTurnStore.HistorySnapshot(1, List.of()));
        when(client.ask(any(), anyString())).thenReturn(new AskResponse(conversation.getIdConversa().toString(), "Resposta"));
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .when(turns).append(any(), anyLong(), anyString(), anyString());
        assertThatThrownBy(() -> service.askOriento("Pergunta", conversation.getIdConversa().toString(), user, "jwt"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private AIConversation existing(Usuario owner) {
        AIConversation conversation = new AIConversation(UUID.randomUUID(), owner);
        conversation.setStatus(StatusConversa.ativa);
        when(conversations.findById(conversation.getIdConversa())).thenReturn(Optional.of(conversation));
        return conversation;
    }
}
