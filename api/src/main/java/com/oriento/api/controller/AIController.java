package com.oriento.api.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.oriento.api.dto.AskResponse;
import com.oriento.api.exception.LlmApiException;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.UsuarioRepository;
import com.oriento.api.services.AIService;
import com.oriento.api.services.ContextoFinanceiroService;
import com.oriento.api.services.ContextoFinanceiroService.CachedContexto;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/oriento")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    private final AIService AIService;
    private final ContextoFinanceiroService contextoFinanceiroService;
    private final UsuarioRepository usuarioRepository;

    public AIController(AIService AIService,
                        ContextoFinanceiroService contextoFinanceiroService,
                        UsuarioRepository usuarioRepository) {
        this.AIService = AIService;
        this.contextoFinanceiroService = contextoFinanceiroService;
        this.usuarioRepository = usuarioRepository;
        logger.info("AIController inicializado com sucesso");
    }

    @PostMapping("/ask")
    public AskResponse askAIApi(
            @RequestBody String prompt,
            @RequestParam(required = false) String conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("Recebida requisição para o assistente Oriento");
        logger.debug("Prompt: {}", prompt);

        Usuario usuario = resolverUsuario(jwt);
        logger.debug("Usuário autenticado: {}", usuario.getIdUsuario());

        AskResponse resposta = AIService.askOriento(prompt, conversationId, usuario);

        logger.info("Resposta do Oriento gerada com sucesso");
        logger.debug("ID da conversa utilizado: {}", resposta != null ? resposta.conversationId() : null);
        logger.debug("Tamanho da resposta: {} caracteres",
                resposta != null && resposta.response() != null ? resposta.response().length() : 0);

        return resposta;
    }

    /**
     * Endpoint streaming via Server-Sent Events.
     *
     * <p>Eventos emitidos:</p>
     * <ul>
     *   <li>{@code start}: {@code { conversationId }} — antes do primeiro delta</li>
     *   <li>{@code delta}: {@code { content }} — vários, conforme tokens chegam</li>
     *   <li>{@code done}:  {@code { conversationId }} — após persistência</li>
     *   <li>{@code error}: {@code { message }} — em caso de falha</li>
     * </ul>
     */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> askStream(
            @RequestBody String prompt,
            @RequestParam(required = false) String conversationId,
            @AuthenticationPrincipal Jwt jwt) {

        logger.info("Recebida requisição streaming para o assistente Oriento");

        Usuario usuario = resolverUsuario(jwt);

        AtomicReference<String> idRef = new AtomicReference<>();
        Flux<ServerSentEvent<Map<String, Object>>> body = AIService
                .askOrientoStream(prompt, conversationId, usuario,
                        idRef::set,
                        full -> {/* persistência já feita no service */})
                .map(delta -> sse("delta", Map.of("content", delta)));

        // start é emitido como primeiro item; done como último.
        Flux<ServerSentEvent<Map<String, Object>>> start = Flux.defer(() -> {
            String id = idRef.get();
            Map<String, Object> payload = new HashMap<>();
            if (id != null) {
                payload.put("conversationId", id);
            }
            return Flux.just(sse("start", payload));
        });

        Flux<ServerSentEvent<Map<String, Object>>> done = Flux.defer(() -> {
            Map<String, Object> payload = new HashMap<>();
            String id = idRef.get();
            if (id != null) {
                payload.put("conversationId", id);
            }
            return Flux.just(sse("done", payload));
        });

        return body.startWith(start)
                .concatWith(done)
                .onErrorResume(err -> {
                    logger.error("Falha no stream do Oriento", err);
                    String message = err instanceof LlmApiException
                            ? err.getMessage()
                            : "Falha temporária ao falar com o assistente. Tente novamente em instantes.";
                    return Flux.just(sse("error", Map.of("message", message)));
                });
    }

    private static ServerSentEvent<Map<String, Object>> sse(String name, Map<String, Object> data) {
        return ServerSentEvent.<Map<String, Object>>builder()
                .event(name)
                .data(data)
                .build();
    }

    /**
     * Atualiza o cache do contexto financeiro do usuário (resumo gerado a partir
     * dos dashboards). Esse contexto é injetado como system message ao iniciar
     * novas conversas com o LLM.
     */
    @PostMapping("/contexto/refresh")
    public ResponseEntity<Map<String, Object>> refreshContexto(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = resolverUsuario(jwt);
        logger.info("Refresh de contexto financeiro solicitado por usuário {}", usuario.getIdUsuario());

        CachedContexto registro = contextoFinanceiroService.gerar(usuario);
        Instant geradoEm = registro != null ? registro.geradoEm() : Instant.now();

        return ResponseEntity.ok(Map.of(
                "geradoEm", geradoEm.toString(),
                "validoPorHoras", 24
        ));
    }

    private Usuario resolverUsuario(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }

        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));
    }

}
