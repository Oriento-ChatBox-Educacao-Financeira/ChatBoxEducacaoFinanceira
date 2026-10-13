package com.oriento.api.services;

import com.oriento.api.client.LlmApiClient;
import com.oriento.api.dto.AskResponse;
import com.oriento.api.dto.llm.ChatMessage;
import com.oriento.api.model.AIConversation;
import com.oriento.api.model.Mensagens;
import com.oriento.api.model.Usuario;
import com.oriento.api.repositories.AIConversationRepository;
import com.oriento.api.repositories.MensagensRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private static final String SYSTEM_INSTRUCTION = """
            Você é o Oriento, um assistente de educação e gestão financeira para pequenas e médias empresas (PMEs).
            Sempre se apresente como Oriento e responda em português brasileiro claro e natural.

            Comporte-se como um consultor financeiro confiável, empático e prático:
            - Respostas curtas (1 a 3 parágrafos), focadas em ações concretas.
            - Use **negrito** para termos-chave e listas com `-` quando ajudar a leitura.
            - Explique termos técnicos com analogias simples, sem ser acadêmico.

            Escopo: finanças empresariais, fluxo de caixa, DRE, balanço, indicadores, custos, margem,
            orçamento, planejamento e crescimento. Para temas fora desse escopo, redirecione gentilmente.

            Se houver um bloco "CONTEXTO_FINANCEIRO" no histórico, use os números dele como referência
            real do usuário ao recomendar ações. Não invente dados se o contexto não estiver disponível.
            """;

    private static final String REMETENTE_USUARIO = "usuario";
    private static final String REMETENTE_IA = "ia";
    private static final String TIPO_TEXTO = "texto";

    private final LlmApiClient llmApiClient;
    private final AIConversationRepository conversationRepository;
    private final MensagensRepository mensagensRepository;
    private final ContextoFinanceiroService contextoFinanceiroService;
    private final Map<String, List<ChatMessage>> conversationHistory = new ConcurrentHashMap<>();
    private final int maxHistoryMessages;

    public AIService(LlmApiClient llmApiClient,
                     AIConversationRepository conversationRepository,
                     MensagensRepository mensagensRepository,
                     ContextoFinanceiroService contextoFinanceiroService,
                     @Value("${llm.api.history.max-messages:20}") int maxHistoryMessages) {
        this.llmApiClient = llmApiClient;
        this.conversationRepository = conversationRepository;
        this.mensagensRepository = mensagensRepository;
        this.contextoFinanceiroService = contextoFinanceiroService;
        this.maxHistoryMessages = Math.max(2, maxHistoryMessages);
        logger.info("AIService inicializado (modo síncrono + streaming, max_history={})",
                this.maxHistoryMessages);
    }

    /**
     * Versão síncrona — espera a resposta inteira e devolve. Mantida para
     * clientes legados; usa o stream internamente e bufferiza.
     */
    public AskResponse askOriento(String prompt, String conversationId, Usuario usuario) {
        logger.info("Processando pergunta síncrona para o Oriento");
        StringBuilder buffer = new StringBuilder();
        String[] effectiveId = new String[1];

        Flux<String> stream = askOrientoStream(prompt, conversationId, usuario,
                id -> effectiveId[0] = id,
                full -> {/* já persistido pelo stream */});
        stream.toIterable().forEach(buffer::append);

        return new AskResponse(effectiveId[0], buffer.toString());
    }

    /**
     * Versão streaming — emite cada delta de conteúdo. Persiste a mensagem do
     * usuário antes de iniciar o stream e a resposta completa do assistente
     * ao terminar (ou ao falhar com a porção parcial recebida).
     *
     * @param onConversationCreated callback síncrono com o conversationId,
     *                               disparado antes do primeiro delta.
     * @param onComplete             callback com a resposta completa, após
     *                               persistência no Mongo.
     */
    public Flux<String> askOrientoStream(String prompt,
                                         String conversationId,
                                         Usuario usuario,
                                         Consumer<String> onConversationCreated,
                                         Consumer<String> onComplete) {
        logger.info("Iniciando streaming do Oriento");
        logger.debug("Prompt recebido: {}", prompt);

        AIConversation conversation = resolveConversation(conversationId, usuario);
        UUID conversaUuid = conversation.getIdConversa();
        String effectiveConversationId = String.valueOf(conversaUuid);

        if (onConversationCreated != null) {
            try {
                onConversationCreated.accept(effectiveConversationId);
            } catch (RuntimeException ex) {
                logger.warn("onConversationCreated falhou: {}", ex.getMessage());
            }
        }

        List<ChatMessage> history = conversationHistory.computeIfAbsent(
                effectiveConversationId,
                id -> reidratarOuCriarHistorico(conversaUuid, usuario));

        history.add(ChatMessage.user(prompt));
        truncarHistorico(history);

        long ordemBase = mensagensRepository.countByConversaId(conversaUuid);
        persistirMensagem(conversaUuid, REMETENTE_USUARIO, prompt, (int) (ordemBase + 1));

        StringBuilder respostaCompleta = new StringBuilder();

        return llmApiClient.chatStream(List.copyOf(history))
                .doOnNext(respostaCompleta::append)
                .doOnComplete(() -> {
                    String resposta = respostaCompleta.toString();
                    history.add(ChatMessage.assistant(resposta));
                    truncarHistorico(history);
                    persistirMensagem(conversaUuid, REMETENTE_IA, resposta, (int) (ordemBase + 2));
                    logger.info("Streaming concluído para conversa {} ({} chars)",
                            effectiveConversationId, resposta.length());
                    if (onComplete != null) {
                        try {
                            onComplete.accept(resposta);
                        } catch (RuntimeException ex) {
                            logger.warn("onComplete falhou: {}", ex.getMessage());
                        }
                    }
                })
                .doOnError(err -> {
                    logger.error("Erro no streaming da conversa {}: {}",
                            effectiveConversationId, err.getMessage());
                    String parcial = respostaCompleta.toString();
                    if (!parcial.isEmpty()) {
                        history.add(ChatMessage.assistant(parcial));
                        truncarHistorico(history);
                        persistirMensagem(conversaUuid, REMETENTE_IA, parcial, (int) (ordemBase + 2));
                    }
                });
    }

    /**
     * Trunca o histórico mantendo TODAS as mensagens system iniciais e
     * apenas as últimas {@code maxHistoryMessages} mensagens user/assistant.
     */
    private void truncarHistorico(List<ChatMessage> history) {
        int systemPrefix = 0;
        for (ChatMessage m : history) {
            if ("system".equals(m.role())) {
                systemPrefix++;
            } else {
                break;
            }
        }
        int permitido = systemPrefix + maxHistoryMessages;
        if (history.size() > permitido) {
            int removerAte = history.size() - permitido;
            history.subList(systemPrefix, systemPrefix + removerAte).clear();
        }
    }

    private List<ChatMessage> reidratarOuCriarHistorico(UUID conversaId, Usuario usuario) {
        List<ChatMessage> historico = new ArrayList<>();
        historico.add(ChatMessage.system(SYSTEM_INSTRUCTION));

        String contexto = contextoFinanceiroService.obter(usuario);
        if (StringUtils.hasText(contexto)) {
            historico.add(ChatMessage.system("CONTEXTO_FINANCEIRO:\n" + contexto));
            logger.debug("Contexto financeiro injetado no system prompt da conversa {}", conversaId);
        }

        List<Mensagens> persistidas = mensagensRepository.findByConversaIdOrderByOrdemAsc(conversaId);
        if (!persistidas.isEmpty()) {
            logger.info("Reidratando até {} mensagens persistidas da conversa {}",
                    maxHistoryMessages, conversaId);
            int inicio = Math.max(0, persistidas.size() - maxHistoryMessages);
            for (int i = inicio; i < persistidas.size(); i++) {
                Mensagens m = persistidas.get(i);
                String texto = m.getConteudo() != null ? m.getConteudo().texto : null;
                if (!StringUtils.hasText(texto)) {
                    continue;
                }
                if (REMETENTE_USUARIO.equals(m.getRemetente())) {
                    historico.add(ChatMessage.user(texto));
                } else if (REMETENTE_IA.equals(m.getRemetente())) {
                    historico.add(ChatMessage.assistant(texto));
                }
            }
        } else {
            logger.debug("Nenhum histórico persistido para a conversa {} — sessão nova", conversaId);
        }

        return historico;
    }

    private void persistirMensagem(UUID conversaId, String remetente, String texto, int ordem) {
        try {
            Mensagens m = new Mensagens();
            m.setConversaId(conversaId);
            m.setRemetente(remetente);
            m.setTipoMensagem(TIPO_TEXTO);
            m.setOrdem(ordem);
            Mensagens.Conteudo conteudo = new Mensagens.Conteudo();
            conteudo.texto = texto;
            m.setConteudo(conteudo);
            mensagensRepository.save(m);
        } catch (RuntimeException ex) {
            logger.error("Falha ao persistir mensagem (conversa={}, remetente={}): {}",
                    conversaId, remetente, ex.getMessage());
        }
    }

    private AIConversation resolveConversation(String conversationId, Usuario usuario) {
        if (!StringUtils.hasText(conversationId)) {
            UUID generatedConversationId = UUID.randomUUID();
            AIConversation nova = new AIConversation(generatedConversationId, usuario);
            nova.setIniciadaEm(OffsetDateTime.now());
            AIConversation persistida = conversationRepository.save(nova);
            logger.debug("Nova conversa {} criada para o usuário {}", generatedConversationId,
                    usuario.getIdUsuario());
            return persistida;
        }

        UUID conversaUuid;
        try {
            conversaUuid = UUID.fromString(conversationId);
        } catch (IllegalArgumentException ex) {
            throw new EntityNotFoundException("ID de conversa inválido: " + conversationId);
        }

        AIConversation existing = conversationRepository.findById(conversaUuid)
                .orElseThrow(() -> new EntityNotFoundException("Conversa não encontrada para o ID informado"));

        if (!existing.pertenceAo(usuario)) {
            throw new AccessDeniedException("Conversa não pertence ao usuário autenticado");
        }

        return existing;
    }

    /**
     * Lista as conversas do usuário ordenadas pela mais recente.
     * Cada item inclui um título derivado da primeira mensagem do usuário.
     */
    public List<ConversaResumo> listarConversas(Usuario usuario) {
        List<AIConversation> conversas = conversationRepository
                .findByUsuario_IdUsuarioOrderByIniciadaEmDesc(usuario.getIdUsuario());

        List<ConversaResumo> resumos = new ArrayList<>();
        for (AIConversation c : conversas) {
            UUID id = c.getIdConversa();
            String titulo = mensagensRepository
                    .findFirstByConversaIdAndRemetenteOrderByOrdemAsc(id, REMETENTE_USUARIO)
                    .map(m -> m.getConteudo() != null ? m.getConteudo().texto : null)
                    .map(this::resumirTitulo)
                    .orElse("Conversa em branco");
            OffsetDateTime ultimaInteracao = mensagensRepository
                    .findFirstByConversaIdOrderByOrdemDesc(id)
                    .map(m -> m.getDataHora() != null
                            ? m.getDataHora().atOffset(OffsetDateTime.now().getOffset())
                            : null)
                    .orElse(c.getIniciadaEm());

            resumos.add(new ConversaResumo(id, titulo, c.getIniciadaEm(), ultimaInteracao));
        }
        return resumos;
    }

    private String resumirTitulo(String texto) {
        if (!StringUtils.hasText(texto)) {
            return "Conversa em branco";
        }
        String limpo = texto.replaceAll("\\s+", " ").trim();
        if (limpo.length() <= 60) {
            return limpo;
        }
        return limpo.substring(0, 57) + "…";
    }

    public boolean conversaPertenceAoUsuario(UUID conversaId, Usuario usuario) {
        return conversationRepository.findById(conversaId)
                .map(c -> c.pertenceAo(usuario))
                .orElse(false);
    }

    public record ConversaResumo(UUID id, String titulo, OffsetDateTime iniciadaEm,
                                 OffsetDateTime ultimaInteracao) {}
}
