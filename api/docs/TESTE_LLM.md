# Integração Oriento: Java → Python → Ollama

O frontend continua chamando `POST /api/oriento/ask` no Java.
Java autentica o usuário, autoriza a conversa, resolve sua empresa, carrega o histórico
do MongoDB e encaminha a geração ao projeto irmão `chatbox-ai`.
Python consulta os indicadores no Java e chama Ollama. Java salva o turno antes de responder.

## Preparação

Requer Java 21+, Maven, PostgreSQL com o schema do projeto, MongoDB, Python 3.12+ e Ollama.
O `compose.yaml` antigo contém MySQL e não representa esta integração com PostgreSQL/MongoDB.

1. Configure os bancos do Java com `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
   `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_MONGODB_URI` e `SPRING_DATA_MONGODB_DATABASE`.
   As propriedades padrão existentes do projeto continuam válidas; use variáveis para selecionar o ambiente.
2. Execute a migração no PostgreSQL escolhido, antes de habilitar a IA:

```powershell
cd chatbox-backend/api
psql -h localhost -U seu_usuario -d oriento -f docs/migrations/001_conversa_empresa_opcional.sql
```

A migração remove apenas a obrigatoriedade de `conversa.id_empresa`, preservando os IDs e dados.
É repetível. Não executamos essa migração automaticamente no banco configurado.
Os dois mapeamentos Java da tabela `conversa` passam a permitir empresa nula.

3. Instale e inicie Python conforme `../../chatbox-ai/README.md` a partir da pasta `api`
   (o projeto Python fica dois níveis acima de `api`, ao lado de `chatbox-backend`).
4. Inicie Ollama em `127.0.0.1:11435` e instale `deepseek-r1:8b`.
5. Configure o segredo compartilhado e ative o proxy:

```powershell
$env:ORIENTO_INTERNAL_TOKEN = "seu-segredo-compartilhado"
$env:ORIENTO_AI_BASE_URL = "http://127.0.0.1:8000"
$env:ORIENTO_AI_ENABLED = "true"
mvn spring-boot:run
# Alternativa, se disponível neste checkout: .\mvnw.cmd spring-boot:run
```

No `chatbox-ai/.env`, `INTERNAL_TOKEN` deve ter o mesmo valor de `ORIENTO_INTERNAL_TOKEN`.
O exemplo `api/.env.example` documenta as variáveis Java; Spring não carrega arquivos
`.env` automaticamente. Exporte as variáveis no shell ou na configuração do processo.

| Variável Java | Padrão |
| --- | --- |
| `ORIENTO_AI_ENABLED` | `false` |
| `ORIENTO_AI_BASE_URL` | `http://127.0.0.1:8000` |
| `ORIENTO_INTERNAL_TOKEN` | vazio; obrigatório com IA habilitada |
| `ORIENTO_AI_CONNECT_TIMEOUT` | `5s` |
| `ORIENTO_AI_READ_TIMEOUT` | `150s` |

A flag desligada retorna 503 sem criar conversa nem chamar Python.
IA habilitada sem segredo impede a inicialização. A flag é lida na inicialização;
reinicie o Java ao alterá-la. O Java não chama mais Ollama diretamente.

## Contrato público preservado

Use um JWT aceito pelo Java, cujo `sub` seja o UUID de um usuário registrado.

```http
POST http://localhost:8080/api/oriento/ask
Authorization: Bearer JWT-do-usuario
Content-Type: text/plain

Explique como melhorar o fluxo de caixa.
```

Resposta:

```json
{
  "conversationId": "uuid-da-conversa",
  "response": "texto do Oriento"
}
```

Para continuar, use `POST /api/oriento/ask?conversationId={uuid}`.
O frontend não precisa alterar corpo, URL ou formato de resposta.
Prompt vazio e UUID malformado retornam 400; conversa inexistente, 404;
conversa de outro usuário, 403; conversa encerrada ou conflito entre turnos, 409.
O JWT encaminhado ao Python é o token recebido na chamada pública.

## Indicadores internos

```http
GET http://localhost:8080/internal/empresas/7/indicadores
X-Internal-Token: segredo-compartilhado
Authorization: Bearer JWT-do-usuario
```

Retorna os últimos 12 períodos cadastrados, do mais antigo para o mais recente:

```json
{
  "empresaId": 7,
  "indicadores": [
    {
      "idIndicador": 1,
      "anoMes": "2026-09-01",
      "receitaTotal": "12500.00",
      "despesasTotais": "9000.00",
      "lucroLiquido": "3500.00",
      "margemLucro": "28.00",
      "fluxoCaixa": null
    }
  ]
}
```

Decimais são strings, datas são ISO e ausência de dado permanece nula.
O endpoint projeta somente os indicadores e exige ambas as credenciais.
Empresa inexistente retorna 404; empresa de outro usuário, 403.
Empresa existente sem indicadores retorna lista vazia.
O projeto considera uma empresa por usuário, conforme o relacionamento atual.

## Persistência e falhas

A coleção `oriento_turnos` reutiliza a conexão MongoDB do Java.
Cada documento contém `conversationId` como UUID textual, `ordem`, `pergunta`,
`resposta` e `criadoEm`. O índice único `uk_oriento_conversa_ordem` é criado
durante a inicialização com a flag ligada.

Pergunta e resposta são inseridas no mesmo documento após a geração.
O histórico completo fica armazenado; somente os últimos 20 turnos entram no contexto.
Duas chamadas que leem a mesma próxima ordem disputam o índice: uma grava e outra recebe 409.
Não há repetição automática da geração.

Falha de geração não grava turno; falha de armazenamento impede retorno de sucesso.
Uma primeira chamada que falhe pode deixar uma conversa vazia no PostgreSQL.
A criação da conversa e a gravação do turno usam bancos distintos, sem transação distribuída.
O contexto anteriormente mantido apenas em memória não é migrável; conversas existentes
conservam seus IDs e passam a acumular novos turnos persistidos.

A coleção de turnos atende o contexto do assistente. As APIs existentes de `mensagens`
continuam usando sua coleção própria; não passam a listar esses turnos automaticamente.

Timeout, conexão indisponível, HTTP 429 ou 5xx ao consultar indicadores permitem uma
resposta genérica com aviso explícito. Erros de autenticação e autorização interrompem a geração.
Indisponibilidade de Python/Ollama retorna 503; resposta incoerente do Python retorna 502.
Os logs da integração registram duração e códigos de falha sem conteúdo da conversa ou tokens.

## Testes

```powershell
cd chatbox-backend/api
mvn test
```

Os testes de contratos usam dependências simuladas. Com Docker disponível,
os testes de persistência iniciam PostgreSQL e MongoDB descartáveis via Testcontainers.
Sem Docker e sem os endereços de teste, a classe de integração é abortada com indicação do requisito.

Para executar a mesma integração no Windows com binários locais e dados novos:

```powershell
.\scripts\test-local-databases.ps1 `
  -PostgresBin "C:\Program Files\PostgreSQL\18\bin" `
  -MongoExecutable "C:\caminho\mongodb\bin\mongod.exe" `
  -MavenCommand "mvn"
```

O executor usa exclusivamente as portas 54394/27094, verifica se estão livres, cria
um cluster em `target/local-database-tests` e encerra seus processos ao terminar.
Os logs e diretórios temporários ficam nesse diretório ignorado pelo Git.
Pode alterar portas com `-PostgresPort` e `-MongoPort`.

Também é possível fornecer `ORIENTO_TEST_JDBC_URL`, `ORIENTO_TEST_JDBC_USER`,
`ORIENTO_TEST_JDBC_PASSWORD` e `ORIENTO_TEST_MONGO_URI` para instâncias descartáveis.
Esses testes criam e removem tabelas; use exclusivamente bancos de teste.
O smoke test Java verifica a configuração do proxy sem acessar servidores do ambiente.

Os testes Python estão em `chatbox-ai/tests` e usam transporte HTTP simulado para Java/Ollama.
