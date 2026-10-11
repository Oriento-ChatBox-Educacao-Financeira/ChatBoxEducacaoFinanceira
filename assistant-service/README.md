# Oriento Assistant Service

Serviço Python responsável pelas funcionalidades de IA do Oriento.

## Escopo atual

Este serviço foi criado como parte da separação das responsabilidades de IA do backend Java.

No estado atual, ele possui:

- serviço FastAPI;
- healthcheck;
- execução via Docker;
- CI com GitHub Actions;
- validação de JWT emitido pelo backend Java;
- validação de propriedade de conversa;
- dependency reutilizável para proteger futuros endpoints por conversa.

A integração completa com o fluxo `/api/oriento/ask` pertence a uma etapa posterior do projeto.

## Tecnologias

- Python 3.13
- FastAPI
- Uvicorn
- HTTPX
- PyJWT
- Pytest

## Estrutura

```text
assistant-service/
├── app/
│   ├── conversations/
│   │   ├── __init__.py
│   │   ├── authorization.py
│   │   ├── client.py
│   │   └── dependencies.py
│   ├── security/
│   │   ├── __init__.py
│   │   ├── config.py
│   │   ├── dependencies.py
│   │   └── jwt.py
│   └── main.py
├── tests/
│   ├── test_conversation_authorization.py
│   ├── test_conversation_client.py
│   ├── test_conversation_dependencies.py
│   ├── test_health.py
│   ├── test_jwt.py
│   └── test_security_config.py
├── .dockerignore
├── .env.example
├── Dockerfile
├── README.md
└── requirements.txt
```

## Configuração

O serviço utiliza variáveis de ambiente para configuração.

O arquivo `.env.example` documenta os valores esperados, mas o serviço não carrega arquivos `.env` automaticamente.

Variáveis utilizadas atualmente:

```env
JWT_PUBLIC_KEY_PATH=/run/secrets/jwt_public_key.pem
JWT_ISSUER=oriento
JWT_ALGORITHM=RS256
JAVA_API_URL=http://localhost:8080
```

### JWT_PUBLIC_KEY_PATH

Caminho para a chave pública RSA correspondente à chave privada utilizada pelo backend Java para assinar os JWTs.

O serviço Python utiliza somente a chave pública.

### JWT_ISSUER

Issuer esperado no JWT.

Valor padrão:

```text
oriento
```

### JWT_ALGORITHM

Algoritmo RSA utilizado para validação do JWT.

Algoritmos permitidos:

```text
RS256
RS384
RS512
```

Valor padrão:

```text
RS256
```

Algoritmos simétricos ou inseguros não são aceitos.

### JAVA_API_URL

URL base da API Java usada atualmente para validar propriedade de conversa.

Exemplo local:

```text
http://localhost:8080
```

### Windows PowerShell

Exemplo de configuração local:

```powershell
$env:JWT_PUBLIC_KEY_PATH="C:\caminho\para\app.pub"
$env:JWT_ISSUER="oriento"
$env:JWT_ALGORITHM="RS256"
$env:JAVA_API_URL="http://localhost:8080"
```

### Linux/macOS

```bash
export JWT_PUBLIC_KEY_PATH=/caminho/para/app.pub
export JWT_ISSUER=oriento
export JWT_ALGORITHM=RS256
export JAVA_API_URL=http://localhost:8080
```

## Executar localmente

Entre na pasta do serviço:

```bash
cd assistant-service
```

Crie uma virtual environment:

```bash
python -m venv .venv
```

### Windows PowerShell

```powershell
.\.venv\Scripts\Activate.ps1
```

### Linux/macOS

```bash
source .venv/bin/activate
```

Instale as dependências:

```bash
python -m pip install -r requirements.txt
```

Execute o serviço:

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 8001
```

O serviço ficará disponível em:

```text
http://localhost:8001
```

## Healthcheck

Endpoint:

```http
GET /health
```

Resposta esperada:

```json
{
  "status": "ok",
  "service": "oriento-assistant"
}
```

No PowerShell:

```powershell
Invoke-RestMethod http://localhost:8001/health
```

## Autenticação JWT

O backend Java continua responsável por emitir os tokens JWT.

O serviço Python valida:

- assinatura RSA;
- `issuer`;
- expiração;
- data de emissão;
- presença do `subject`;
- formato UUID do usuário.

Claims obrigatórias atualmente:

```text
sub
iss
iat
exp
```

O campo `sub` deve conter o UUID do usuário.

Claims adicionais, como:

```text
empresaId
email
roles
```

podem ser incluídas pelo Java sem quebrar a validação atual.

## Propriedade de conversa

A proteção de conversa é dividida em três camadas.

### ConversationOwnershipClient

Arquivo:

```text
app/conversations/client.py
```

Responsável pela comunicação com a API Java.

No contrato provisório atual, a validação utiliza:

```http
GET /api/mensagens/conversa/{conversationId}
```

enviando o JWT original no header:

```http
Authorization: Bearer <token>
```

Interpretação atual:

```text
200 -> acesso permitido
401 -> token rejeitado
403 -> acesso negado
404 -> conversa inexistente
5xx -> falha no serviço de autorização
```

Esse contrato pode ser alterado futuramente sem modificar a regra principal de autorização.

### verify_conversation_ownership

Arquivo:

```text
app/conversations/authorization.py
```

Responsável por transformar a resposta da integração em regras da API Python.

Comportamento:

```text
acesso permitido -> continua execução
token rejeitado -> HTTP 401
conversa de outro usuário -> HTTP 404
conversa inexistente -> HTTP 404
API Java indisponível -> HTTP 503
```

O uso de `404` para conversas sem autorização evita confirmar a existência de recursos pertencentes a outros usuários.

### require_conversation_access

Arquivo:

```text
app/conversations/dependencies.py
```

Dependency FastAPI preparada para proteger futuros endpoints relacionados a conversas.

Ela:

1. exige Bearer token;
2. valida o JWT;
3. obtém o UUID do usuário;
4. verifica a propriedade da conversa;
5. retorna o UUID do usuário autenticado.

Exemplo de uso futuro:

```python
from uuid import UUID

from fastapi import Depends

from app.conversations.dependencies import require_conversation_access


async def example_endpoint(
    user_id: UUID = Depends(require_conversation_access),
):
    return {"user_id": str(user_id)}
```

O endpoint definitivo do assistente não faz parte do escopo atual deste serviço.

## Testes

Execute:

```bash
python -m pytest -v
```

Os testes cobrem atualmente:

- healthcheck;
- JWT válido;
- token expirado;
- assinatura inválida;
- issuer inválido;
- `sub` inválido;
- configuração de issuer;
- configuração de algoritmo JWT;
- rejeição de algoritmos não permitidos;
- autorização de conversa;
- negação de conversa;
- token rejeitado pelo Java;
- indisponibilidade da API Java;
- cliente HTTP de propriedade;
- dependency de acesso à conversa.

## Docker

### Build

A partir da raiz do repositório:

```bash
docker build -t oriento-assistant assistant-service
```

Ou dentro de `assistant-service`:

```bash
docker build -t oriento-assistant .
```

### Execução

A chave pública precisa estar disponível dentro do container.

Exemplo conceitual:

```bash
docker run --rm \
  -p 8001:8001 \
  -v /caminho/local/app.pub:/run/secrets/jwt_public_key.pem:ro \
  -e JWT_PUBLIC_KEY_PATH=/run/secrets/jwt_public_key.pem \
  -e JWT_ISSUER=oriento \
  -e JWT_ALGORITHM=RS256 \
  -e JAVA_API_URL=http://host.docker.internal:8080 \
  oriento-assistant
```

A forma definitiva de disponibilização da chave pública depende da infraestrutura utilizada no deploy.

## CI

O projeto possui workflow do GitHub Actions em:

```text
.github/workflows/python-assistant-ci.yml
```

O pipeline executa:

- instalação das dependências;
- testes automatizados;
- build da imagem Docker.

O workflow é acionado quando existem alterações relacionadas ao serviço Python ou ao próprio workflow.

## Responsabilidades entre Java e Python

### Backend Java

Continua responsável por:

- autenticação principal;
- emissão de JWT;
- regras transacionais;
- persistência principal;
- autorização sobre dados de negócio;
- contexto financeiro;
- gerenciamento das conversas persistidas.

### Serviço Python

Responsável atualmente por:

- infraestrutura do serviço de IA;
- validação de JWT;
- proteção reutilizável de acesso a conversas;
- preparação para integração futura com modelos de linguagem.

## Integrações futuras

Algumas decisões pertencem a outras etapas do projeto e podem alterar contratos internos sem exigir reestruturação completa deste serviço.

Entre elas:

- contrato definitivo Java → Python;
- integração com `/api/oriento/ask`;
- integração com LLM/Ollama;
- uso de `empresaId` e `roles`;
- endpoint interno específico para validação de conversa;
- distribuição de chaves nos ambientes de deploy;
- streaming via SSE ou WebSocket;
- persistência e paridade do histórico do assistente.

## Segurança

Não versione:

- chaves privadas;
- chaves públicas específicas de ambiente, quando não forem destinadas a versionamento;
- `.env`;
- tokens JWT reais;
- credenciais de banco;
- API keys;
- segredos de infraestrutura.

Use `.env.example` apenas com valores de exemplo e configurações não sensíveis.

## Desativar a virtual environment

```bash
deactivate
```
