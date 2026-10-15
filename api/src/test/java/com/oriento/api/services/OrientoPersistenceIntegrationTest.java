package com.oriento.api.services;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.oriento.api.config.OrientoMongoConfig;
import com.oriento.api.model.*;
import com.oriento.api.model.enuns.Nivelmaturidadefinanceira;
import com.oriento.api.model.enuns.StatusConversa;
import com.oriento.api.repositories.AIConversationRepository;
import com.oriento.api.repositories.EmpresaRepository;
import com.oriento.api.repositories.IndicadorFinanceiroRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.*;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.*;

class OrientoPersistenceIntegrationTest {
    static PostgreSQLContainer<?> postgres;
    static MongoDBContainer mongo;
    static String jdbcUrl;
    static String jdbcUser;
    static String jdbcPassword;
    static MongoClient mongoClient;
    static MongoTemplate template;
    static SessionFactory sessions;

    @BeforeAll
    static void startDatabases() {
        jdbcUrl = System.getenv("ORIENTO_TEST_JDBC_URL");
        String mongoUri = System.getenv("ORIENTO_TEST_MONGO_URI");
        if (jdbcUrl == null || mongoUri == null) {
            Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Requer Docker ou ORIENTO_TEST_JDBC_URL e ORIENTO_TEST_MONGO_URI para bancos descartáveis");
            postgres = new PostgreSQLContainer<>("postgres:16-alpine");
            mongo = new MongoDBContainer("mongo:8.0");
            postgres.start();
            mongo.start();
            jdbcUrl = postgres.getJdbcUrl();
            jdbcUser = postgres.getUsername();
            jdbcPassword = postgres.getPassword();
            mongoUri = mongo.getReplicaSetUrl();
        } else {
            jdbcUser = System.getenv().getOrDefault("ORIENTO_TEST_JDBC_USER", "postgres");
            jdbcPassword = System.getenv().getOrDefault("ORIENTO_TEST_JDBC_PASSWORD", "");
        }
        mongoClient = MongoClients.create(mongoUri);
        template = new MongoTemplate(mongoClient, "oriento_integration_test");
        sessions = new Configuration().addAnnotatedClass(Usuario.class).addAnnotatedClass(Empresa.class)
                .addAnnotatedClass(AIConversation.class).addAnnotatedClass(Conversa.class)
                .addAnnotatedClass(IndicadorFinanceiro.class)
                .setProperty("hibernate.connection.url", jdbcUrl)
                .setProperty("hibernate.connection.username", jdbcUser)
                .setProperty("hibernate.connection.password", jdbcPassword)
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    @BeforeEach
    void resetTurns() throws Exception {
        template.dropCollection(OrientoTurn.class);
        new OrientoMongoConfig().orientoTurnIndex(template).afterPropertiesSet();
    }

    @AfterAll
    static void stopDatabases() {
        if (sessions != null) sessions.close();
        if (mongoClient != null) {
            template.getDb().drop();
            mongoClient.close();
        }
        if (mongo != null) mongo.stop();
        if (postgres != null) postgres.stop();
    }

    @Test
    void storesCompleteTurnAndReloadsItThroughNewClient() {
        UUID conversationId = UUID.randomUUID();
        OrientoTurnStore store = new OrientoTurnStore(template);
        store.append(conversationId, 1, "Pergunta", "Resposta");
        OrientoTurn stored = template.findAll(OrientoTurn.class).getFirst();
        assertThat(stored.pergunta()).isEqualTo("Pergunta");
        assertThat(stored.resposta()).isEqualTo("Resposta");
        assertThat(stored.criadoEm()).isNotNull();
        String uri = System.getenv("ORIENTO_TEST_MONGO_URI");
        if (uri == null) uri = mongo.getReplicaSetUrl();
        try (MongoClient restartedClient = MongoClients.create(uri)) {
            OrientoTurnStore restartedStore = new OrientoTurnStore(
                    new MongoTemplate(restartedClient, "oriento_integration_test"));
            var snapshot = restartedStore.load(conversationId);
            assertThat(snapshot.nextOrder()).isEqualTo(2);
            assertThat(snapshot.messages()).extracting(item -> item.content()).containsExactly("Pergunta", "Resposta");
        }
    }

    @Test
    void limitsContextTo20TurnsWithoutDeletingOlderDataOrMixingConversations() {
        UUID conversationId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        OrientoTurnStore store = new OrientoTurnStore(template);
        for (int i = 1; i <= 25; i++) store.append(conversationId, i, "P" + i, "R" + i);
        store.append(otherId, 1, "Outra conversa", "Outra resposta");
        var snapshot = store.load(conversationId);
        assertThat(snapshot.nextOrder()).isEqualTo(26);
        assertThat(snapshot.messages()).hasSize(40);
        assertThat(snapshot.messages().getFirst().content()).isEqualTo("P6");
        assertThat(snapshot.messages().getLast().content()).isEqualTo("R25");
        assertThat(template.findAll(OrientoTurn.class)).hasSize(26);
    }

    @Test
    void simultaneousAppendsHaveOneWinnerAndOneConflict() throws Exception {
        UUID id = UUID.randomUUID();
        OrientoTurnStore store = new OrientoTurnStore(template);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (ExecutorService workers = Executors.newFixedThreadPool(2)) {
            Callable<Integer> append = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                try {
                    store.append(id, 1, "Pergunta", "Resposta");
                    return 200;
                } catch (ResponseStatusException ex) {
                    return ex.getStatusCode().value();
                }
            };
            List<Future<Integer>> results = workers.invokeAll(List.of(append, append));
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(template.findAll(OrientoTurn.class)).hasSize(1);
        assertThat(store.load(id).nextOrder()).isEqualTo(2);
    }

    @Test
    void uuidRepositoryAndNullableCompanySurviveEntityManagerRestart() throws Exception {
        UUID conversationId = UUID.randomUUID();
        try (var entityManager = sessions.createEntityManager()) {
            entityManager.getTransaction().begin();
            Usuario user = new Usuario();
            user.setEmail("test-" + conversationId + "@example.test");
            user.setNome("Teste");
            user.setSenha("test-only");
            user.setNivelMaturidadeUser(Nivelmaturidadefinanceira.BASICO);
            entityManager.persist(user);
            AIConversation conversation = new AIConversation(conversationId, user);
            conversation.setStatus(StatusConversa.ativa);
            conversation.setIniciadaEm(OffsetDateTime.now());
            entityManager.persist(conversation);
            entityManager.getTransaction().commit();
        }
        try (var restarted = sessions.createEntityManager()) {
            var repository = new JpaRepositoryFactory(restarted).getRepository(AIConversationRepository.class);
            var companyRepository = new JpaRepositoryFactory(restarted).getRepository(EmpresaRepository.class);
            var indicatorRepository = new JpaRepositoryFactory(restarted).getRepository(IndicadorFinanceiroRepository.class);
            AIConversation conversation = repository.findById(conversationId).orElseThrow();
            assertThat(conversation.getIdConversa()).isEqualTo(conversationId);
            assertThat(conversation.getEmpresa()).isNull();
            assertThat(conversation.getStatus()).isEqualTo(StatusConversa.ativa);
            assertThat(companyRepository.findByUsuarioIdUsuario(conversation.getUsuario().getIdUsuario())).isEmpty();
            assertThat(indicatorRepository.findTop12ByEmpresaIdOrderByAnoMesDesc(999)).isEmpty();
        }
    }

    @Test
    void companyLookupAndIndicatorProjectionReturnOnlyLatest12Periods() {
        Integer companyId;
        UUID userId;
        try (var entityManager = sessions.createEntityManager()) {
            entityManager.getTransaction().begin();
            Usuario user = new Usuario();
            user.setEmail("indicators-" + UUID.randomUUID() + "@example.test");
            user.setNome("Teste");
            user.setSenha("test-only");
            user.setNivelMaturidadeUser(Nivelmaturidadefinanceira.BASICO);
            entityManager.persist(user);
            userId = user.getIdUsuario();
            Empresa company = new Empresa();
            company.setUsuario(user);
            company.setNomeFantasia("Empresa de teste");
            company.setCnpj(12345678000190L);
            company.setSetor("Comércio");
            entityManager.persist(company);
            companyId = company.getId();
            for (int month = 0; month < 14; month++) {
                IndicadorFinanceiro indicator = new IndicadorFinanceiro();
                indicator.setEmpresa(company);
                indicator.setAnoMes(java.time.LocalDate.of(2025, 1, 1).plusMonths(month));
                indicator.setReceitaTotal(new java.math.BigDecimal("10.00"));
                entityManager.persist(indicator);
            }
            entityManager.getTransaction().commit();
        }
        try (var restarted = sessions.createEntityManager()) {
            var factory = new JpaRepositoryFactory(restarted);
            var companies = factory.getRepository(EmpresaRepository.class);
            var indicators = factory.getRepository(IndicadorFinanceiroRepository.class);
            Usuario user = restarted.find(Usuario.class, userId);
            assertThat(companies.findByUsuarioIdUsuario(userId).orElseThrow().getId()).isEqualTo(companyId);
            var result = new InternalIndicadoresService(companies, indicators).get(companyId, user);
            assertThat(result.indicadores()).hasSize(12);
            assertThat(result.indicadores().getFirst().anoMes()).isEqualTo(java.time.LocalDate.of(2025, 3, 1));
            assertThat(result.indicadores().getLast().anoMes()).isEqualTo(java.time.LocalDate.of(2026, 2, 1));
            assertThat(result.indicadores().getFirst().receitaTotal()).isEqualTo("10.00");
            assertThat(result.indicadores().getFirst().fluxoCaixa()).isNull();
        }
    }


    @Test
    void migrationIsRepeatableAndPreservesExistingConversationIds() throws Exception {
        UUID id = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA migration_test");
            statement.execute("SET search_path TO migration_test");
            statement.execute("CREATE TABLE conversa (id_conversa UUID PRIMARY KEY, id_empresa INTEGER NOT NULL)");
            statement.execute("INSERT INTO conversa VALUES ('" + id + "', 7)");
            String migration = Files.readString(Path.of("docs/migrations/001_conversa_empresa_opcional.sql"));
            statement.execute(migration);
            statement.execute(migration);
            statement.execute("INSERT INTO conversa VALUES ('" + UUID.randomUUID() + "', NULL)");
            try (var rows = statement.executeQuery("SELECT id_conversa FROM conversa WHERE id_empresa = 7")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getObject(1, UUID.class)).isEqualTo(id);
            }
            statement.execute("DROP SCHEMA migration_test CASCADE");
        }
    }
}
