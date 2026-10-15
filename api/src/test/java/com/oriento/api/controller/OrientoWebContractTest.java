package com.oriento.api.controller;

import com.oriento.api.config.AuthConfig;
import com.oriento.api.config.OrientoAiProperties;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.EmpresaIndicadoresResponse;
import com.oriento.api.model.Usuario;
import com.oriento.api.services.*;
import com.oriento.api.services.ratelimit.RateLimitService;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AIController.class, InternalEmpresaController.class}, properties = {
        "oriento.ai.enabled=false", "oriento.ai.base-url=http://localhost:8000",
        "oriento.ai.connect-timeout=5s", "oriento.ai.read-timeout=150s", "oriento.ai.internal-token=test-secret"
})
@Import({AuthConfig.class, OrientoWebContractTest.SecurityBeans.class})
class OrientoWebContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean AIService aiService;
    @MockitoBean AuthenticatedUserService users;
    @MockitoBean InternalIndicadoresService indicators;
    @MockitoBean RateLimitService rateLimit;
    @MockitoBean JwtDecoder decoder;
    final UUID userId = UUID.randomUUID();
    final UUID conversationId = UUID.randomUUID();
    Usuario user;

    @BeforeEach
    void setup() {
        user = new Usuario();
        user.setIdUsuario(userId);
        when(users.resolve(any())).thenReturn(user);
        when(rateLimit.tryConsume(anyString())).thenReturn(true);
    }

    @Test
    void publicAskRequiresJwt() throws Exception {
        mvc.perform(post("/api/oriento/ask").contentType(MediaType.TEXT_PLAIN).content("Pergunta"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(aiService, users);
    }

    @Test
    void preservesFrontendTextBodyQueryParameterAndResponse() throws Exception {
        when(aiService.askOriento(eq("Explique fluxo de caixa"), eq(conversationId.toString()), same(user), anyString()))
                .thenReturn(new AskResponse(conversationId.toString(), "Resposta"));
        mvc.perform(post("/api/oriento/ask").with(jwt().jwt(token -> token.subject(userId.toString())))
                        .param("conversationId", conversationId.toString())
                        .contentType(MediaType.TEXT_PLAIN).content("Explique fluxo de caixa"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.response").value("Resposta"));
    }

    @Test
    void preservesDisabledFlag503AndDoesNotExposeExceptionDetails() throws Exception {
        when(aiService.askOriento(anyString(), isNull(), same(user), anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Assistente desabilitado"));
        mvc.perform(post("/api/oriento/ask").with(jwt()).contentType(MediaType.TEXT_PLAIN).content("Pergunta"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("Assistente desabilitado"));
    }

    @Test
    void internalRouteRequiresServiceTokenEvenWithJwt() throws Exception {
        mvc.perform(get("/internal/empresas/7/indicadores").with(jwt())).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/empresas/7/indicadores").with(jwt())
                        .header("X-Internal-Token", "wrong")).andExpect(status().isUnauthorized());
        verifyNoInteractions(indicators, users);
    }

    @Test
    void internalRouteAlsoRequiresUserJwt() throws Exception {
        mvc.perform(get("/internal/empresas/7/indicadores").header("X-Internal-Token", "test-secret"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(indicators, users);
    }

    @Test
    void internalRouteReturnsProjectionWithBothCredentials() throws Exception {
        when(indicators.get(7, user)).thenReturn(new EmpresaIndicadoresResponse(7, List.of()));
        mvc.perform(get("/internal/empresas/7/indicadores").with(jwt())
                        .header("X-Internal-Token", "test-secret"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.empresaId").value(7))
                .andExpect(jsonPath("$.indicadores").isEmpty());
    }

    @Test
    void foreignCompanyReturns403() throws Exception {
        when(indicators.get(7, user)).thenThrow(new AccessDeniedException("Empresa não pertence ao usuário"));
        mvc.perform(get("/internal/empresas/7/indicadores").with(jwt())
                        .header("X-Internal-Token", "test-secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedCompanyIdReturns400() throws Exception {
        mvc.perform(get("/internal/empresas/invalid/indicadores").with(jwt())
                        .header("X-Internal-Token", "test-secret"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidJwtIsRejectedBySecurityBeforeController() throws Exception {
        when(decoder.decode("invalid")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("Invalid"));
        mvc.perform(post("/api/oriento/ask").header("Authorization", "Bearer invalid")
                        .contentType(MediaType.TEXT_PLAIN).content("Pergunta"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(aiService, users);
    }

    @TestConfiguration
    @EnableConfigurationProperties(OrientoAiProperties.class)
    static class SecurityBeans {
        @Bean KeyPair keyPair() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
        @Bean RSAPublicKey publicKey(KeyPair keys) { return (RSAPublicKey) keys.getPublic(); }
        @Bean RSAPrivateKey privateKey(KeyPair keys) { return (RSAPrivateKey) keys.getPrivate(); }
    }
}
