package com.oriento.api.client;

import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.dto.ai.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrientoAiClientTest {
    MockRestServiceServer server;
    OrientoAiClient client;
    OrientoAskRequest request;

    @BeforeEach
    void setup() {
        RestTemplate rest = new RestTemplate();
        server = MockRestServiceServer.bindTo(rest).build();
        client = new OrientoAiClient(rest, new OrientoAiProperties(true, URI.create("http://localhost:8000/"),
                Duration.ofSeconds(5), Duration.ofSeconds(150), "internal-secret"));
        request = new OrientoAskRequest(UUID.randomUUID(), "Pergunta", 7,
                List.of(new HistoryMessage("user", "Anterior")));
    }

    @Test
    void sendsJsonAndBothAuthenticationHeaders() {
        server.expect(requestTo("http://localhost:8000/internal/oriento/ask"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer user-jwt"))
                .andExpect(header("X-Internal-Token", "internal-secret"))
                .andExpect(jsonPath("$.empresaId").value(7))
                .andExpect(jsonPath("$.history[0].role").value("user"))
                .andRespond(withSuccess("{\"conversationId\":\"" + request.conversationId()
                        + "\",\"response\":\"Resposta\"}", MediaType.APPLICATION_JSON));
        assertThat(client.ask(request, "user-jwt").response()).isEqualTo("Resposta");
        server.verify();
    }

    @Test
    void wrongConversationIdIsRejected() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"conversationId\":\"" + UUID.randomUUID() + "\",\"response\":\"Resposta\"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.ask(request, "jwt")).isInstanceOfSatisfying(
                ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void blankGenerationIsRejected() {
        server.expect(anything()).andRespond(withSuccess(
                "{\"conversationId\":\"" + request.conversationId() + "\",\"response\":\" \"}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.ask(request, "jwt")).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void upstreamFailureDoesNotExposeItsBody() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).body("sensitive upstream"));
        assertThatThrownBy(() -> client.ask(request, "jwt")).isInstanceOfSatisfying(
                ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(ex.getReason()).doesNotContain("sensitive");
                });
    }

    @Test
    void authorizationFailureIsPreserved() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> client.ask(request, "jwt")).isInstanceOfSatisfying(
                ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }
}
