# Tutorial: testar integração LLM local (Oriento)

## Pré-requisitos

1. **MySQL e MongoDB** configurados conforme `application.properties` (variáveis de ambiente ou `.env`).
2. **Ollama** ativo na porta definida em `llm.api.base-url` (padrão: `11435`).

```powershell
$env:OLLAMA_HOST = "127.0.0.1:11435"
ollama serve
```

3. **Modelo instalado** (nome deve coincidir com `llm.api.model`):

```powershell
ollama list
ollama pull deepseek-r1:8b
```

## Passo 1 — Subir a API

```powershell
cd api
.\mvnw.cmd spring-boot:run
```

## Passo 2 — Obter JWT

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "seu@email.com",
  "senha": "sua-senha"
}
```

Copie o campo `accessToken` da resposta.

## Passo 3 — Primeira pergunta (nova conversa)

```http
POST http://localhost:8080/api/oriento/ask
Authorization: Bearer {accessToken}
Content-Type: text/plain

Explique o que é fluxo de caixa para uma PME.
```

Resposta esperada (JSON):

```json
{
  "conversationId": "uuid-gerado",
  "response": "texto do assistente..."
}
```

## Passo 4 — Continuar a mesma conversa

```http
POST http://localhost:8080/api/oriento/ask?conversationId={conversationId}
Authorization: Bearer {accessToken}
Content-Type: text/plain

Quais são os três principais indicadores que devo acompanhar?
```

A resposta deve considerar o contexto da pergunta anterior.

## Passo 5 — Swagger UI

1. Abra `http://localhost:8080/swagger-ui.html`
2. Clique em **Authorize** e informe `Bearer {accessToken}`
3. Use o endpoint **POST /api/oriento/ask** na tag **Assistente Oriento**

## Passo 6 — Teste direto no servidor LLM (isolado)

```powershell
curl http://127.0.0.1:11435/api/chat -Method POST -ContentType "application/json" -Body '{"model":"deepseek-r1:8b","stream":false,"messages":[{"role":"user","content":"Olá"}]}'
```

## Diagnóstico de falhas

| Sintoma | Causa provável | Ação |
|---------|----------------|------|
| Connection refused | Ollama parado ou URL errada | Verificar `ollama serve` e `llm.api.base-url` |
| 404 model not found | Modelo não instalado | `ollama pull {modelo}` |
| Timeout na 1ª chamada | Modelo carregando na memória | Aumentar `llm.api.read-timeout` |
| 401 / 403 | JWT inválido ou conversa de outro usuário | Renovar token ou usar `conversationId` correto |
| 503 | Servidor LLM indisponível | Ver logs da API e do Ollama |

## Configuração opcional por variáveis de ambiente

| Variável | Propriedade | Padrão |
|----------|-------------|--------|
| `LLM_API_BASE_URL` | `llm.api.base-url` | `http://127.0.0.1:11435` |
| `LLM_API_MODEL` | `llm.api.model` | `deepseek-r1:8b` |
