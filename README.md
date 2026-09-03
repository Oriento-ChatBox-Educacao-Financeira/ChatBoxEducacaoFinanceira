# Oriento - Plataforma de Educação Financeira para PMEs

![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen?style=flat&logo=spring)
![Angular](https://img.shields.io/badge/Angular-20-red?style=flat&logo=angular)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat)

## Sobre o Projeto

**Oriento** é uma plataforma web completa de educação financeira voltada para pequenas e médias empresas (PMEs). A aplicação oferece um assistente virtual inteligente alimentado por um modelo de linguagem (LLM) servido via Ollama, que auxilia empreendedores com orientações personalizadas sobre gestão financeira, controle de fluxo de caixa, investimentos e estratégias de crescimento empresarial.

### Principais Funcionalidades

- Assistente Virtual IA (Oriento) especializado em educação financeira empresarial
- Sistema de autenticação seguro com JWT e Refresh Tokens
- Dashboard interativo para gestão de informações financeiras
- Perfil de usuário customizável
- Rate limiting para proteção contra ataques de força bruta
- Interface responsiva e intuitiva

---

## Arquitetura

O projeto segue uma arquitetura **Fullstack Monorepo** com separação clara entre frontend e backend:

```
ChatBoxEducacaoFinanceira/
├── api/                    # Backend - Spring Boot REST API
│   ├── src/main/java/com/oriento/api/
│   │   ├── client/         # Integrações externas (LLM / AI)
│   │   ├── config/         # Configurações (Security, CORS, Rate Limit)
│   │   ├── controller/     # Controllers REST (Auth, AI, Usuario)
│   │   ├── dto/            # Data Transfer Objects
│   │   ├── exception/      # Tratamento global de exceções
│   │   ├── filter/         # Filtros HTTP personalizados
│   │   ├── model/          # Entidades JPA
│   │   ├── repositories/   # Repositórios Spring Data JPA
│   │   └── services/       # Lógica de negócio
│   └── src/main/resources/
│       ├── application.properties
│       └── keys/           # Chaves RSA para JWT
│
└── frontend/               # Frontend - Angular SPA
    └── src/app/
        ├── _components/    # Componentes reutilizáveis
        ├── guards/         # Guards de autenticação
        ├── interceptors/   # Interceptadores HTTP (Auth, Errors)
        ├── models/         # Interfaces TypeScript
        ├── pages/          # Páginas da aplicação
        ├── services/       # Serviços (API, Auth, AI)
        └── validators/     # Validações customizadas
```

---

## Tecnologias Utilizadas

### Backend
- **Java 21** - Linguagem de programação
- **Spring Boot 3.5.6** - Framework principal
- **Spring Security** - Autenticação e autorização
- **Spring Data JPA** - ORM para persistência
- **MySQL** - Banco de dados relacional
- **JWT (JSON Web Tokens)** - Autenticação stateless
- **Bucket4j** - Rate limiting
- **Ollama + LLM local** - Inteligência artificial conversacional (qwen2.5:7b-instruct)
- **Maven** - Gerenciamento de dependências

### Frontend
- **Angular 20** - Framework frontend
- **TypeScript 5.9** - Linguagem de programação
- **RxJS 7.8** - Programação reativa
- **Angular Router** - Roteamento SPA
- **HttpClient** - Comunicação HTTP

### Infraestrutura
- **Git** - Controle de versão
- **Docker Compose** - Orquestração de containers (planejado)

---

## Pré-requisitos

Antes de começar, certifique-se de ter instalado:

- **Java JDK 21** ou superior
- **Node.js 20** ou superior (exigido pelas dependências do `@supabase/supabase-js`)
- **npm** ou **yarn**
- **MySQL 8.0** ou superior
- **Maven 3.8** ou superior (ou use o wrapper incluído)
- **Git**

---

## Instalação e Configuração

### 1. Clone o Repositório

```bash
git clone https://github.com/seu-usuario/ChatBoxEducacaoFinanceira.git
cd ChatBoxEducacaoFinanceira
```

### 2. Configurar Banco de Dados

Crie um banco de dados MySQL:

```sql
CREATE DATABASE oriento CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'orientodb'@'localhost' IDENTIFIED BY 'sua_senha_segura';
GRANT ALL PRIVILEGES ON oriento.* TO 'orientodb'@'localhost';
FLUSH PRIVILEGES;
```

### 3. Configurar Backend

#### 3.1. Configurar Variáveis de Ambiente

Edite o arquivo `api/src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/oriento
spring.datasource.username=orientodb
spring.datasource.password=sua_senha_segura

# LLM local (Ollama) — endpoint e modelo padrão
llm.api.base-url=http://127.0.0.1:11434
llm.api.model=qwen2.5:7b-instruct
```

#### 3.2. Gerar Chaves RSA para JWT

```bash
cd api/src/main/resources/keys
# Gerar chave privada
openssl genrsa -out app.key 2048
# Gerar chave pública
openssl rsa -in app.key -pubout -out app.pub
```

#### 3.3. Compilar e Executar

```bash
cd api
./mvnw clean install
./mvnw spring-boot:run
```

A API estará disponível em: `http://localhost:8080`

### 4. Configurar Frontend

```bash
cd frontend
npm install
npm start
```

A aplicação frontend estará disponível em: `http://localhost:4200`

---

## Endpoints da API

### Documentação Interativa

Acesse a documentação completa e interativa da API via Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

### Principais Endpoints

#### Autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/login` | Autenticar usuário |
| POST | `/refresh` | Renovar access token |

#### Usuários

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/usuario/cadastro` | Registrar novo usuário |
| GET | `/usuario/perfil` | Obter dados do perfil |
| PUT | `/usuario/perfil` | Atualizar perfil |

#### Assistente Oriento

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/oriento/ask` | Fazer pergunta ao assistente |

---

## Segurança

### Autenticação JWT

O sistema utiliza autenticação stateless com JWT (JSON Web Tokens) usando criptografia RSA:

- **Access Token**: Expira em 15 minutos
- **Refresh Token**: Expira em 7 dias
- Tokens assinados com chaves RSA 2048 bits

### Rate Limiting

Proteção contra ataques de força bruta com Bucket4j:

- **Login**: 5 tentativas a cada 15 minutos por IP
- **Endpoints gerais**: 100 requisições por minuto

### Boas Práticas

- Senhas armazenadas com BCrypt
- CORS configurado para domínios específicos
- Validação de entrada com Bean Validation
- Tratamento global de exceções
- Logs de auditoria para ações críticas

---

## Testes

### Backend

```bash
cd api
./mvnw test
```

### Frontend

```bash
cd frontend
npm test
```

---

## Build para Produção

### Backend

```bash
cd api
./mvnw clean package -DskipTests
```

O arquivo `.jar` será gerado em `api/target/api-0.0.1-SNAPSHOT.jar`

### Frontend

```bash
cd frontend
npm run build
```

Os arquivos otimizados serão gerados em `frontend/dist/`

---

## Variáveis de Ambiente

### Backend (application.properties)

| Variável | Descrição | Padrão |
|----------|-----------|--------|
| `spring.datasource.url` | URL do banco MySQL | `jdbc:mysql://localhost:3306/oriento` |
| `spring.datasource.username` | Usuário do banco | `orientodb` |
| `spring.datasource.password` | Senha do banco | - |
| `llm.api.base-url` | URL base do servidor Ollama | `http://127.0.0.1:11434` |
| `llm.api.model` | Modelo LLM a ser usado | `qwen2.5:7b-instruct` |
| `jwt.public.key` | Caminho da chave pública JWT | `classpath:keys/app.pub` |
| `jwt.private.key` | Caminho da chave privada JWT | `classpath:keys/app.key` |

---

## Contribuindo

### Setup inicial dos git hooks

Após clonar o repositório, rode **uma única vez** na raiz do projeto:

```bash
npm install
```

Isso instala o `husky` e ativa um hook local que:

- Bloqueia push direto em `main` ou `develop`.
- Valida o nome da sua branch antes do push.

Se você pular esse passo, a proteção continua valendo (rulesets do GitHub bloqueiam no servidor), mas você só recebe o erro depois de tentar pushar, pior experiência.

### Fluxo

1. Crie uma branch a partir da `main` seguindo o padrão de naming abaixo.
2. Faça seus commits seguindo Conventional Commits.
3. Abra um Pull Request para `main`.
4. O PR precisa de **1 aprovação** e todos os comentários de review **resolvidos** antes do merge.

### Padrão de nomes de branch

Formato: `tipo/descricao-em-kebab-case`

| Tipo | Quando usar |
|---|---|
| `feat` ou `feature` | Nova funcionalidade |
| `fix` ou `bugfix` | Correção de bug |
| `hotfix` | Fix urgente em produção |
| `refactor` | Refatoração sem mudar comportamento |
| `docs` | Documentação |
| `chore` | Manutenção, configuração, tooling |
| `test` | Adicionar/ajustar testes |
| `style` | Ajustes visuais / formatação |
| `release` | Branch de release |

**Exemplos válidos:** `feat/login-google`, `fix/correcao-rotas`, `refactor/tela-login`, `docs/atualiza-readme`.

**Exemplos inválidos:** `feature/NovaFuncionalidade` (camelCase), `minha-branch` (sem tipo), `feat/Login Social` (espaço).

### Padrão de Commits

Seguimos o [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` Nova funcionalidade
- `fix:` Correção de bug
- `docs:` Documentação
- `style:` Formatação de código
- `refactor:` Refatoração de código
- `test:` Testes
- `chore:` Manutenção

---

## Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## Contato

Para dúvidas ou sugestões, entre em contato:

- **Repositório**: [GitHub](https://github.com/seu-usuario/ChatBoxEducacaoFinanceira)
- **Issues**: [GitHub Issues](https://github.com/seu-usuario/ChatBoxEducacaoFinanceira/issues)

---

**Desenvolvido com dedicação para ajudar PMEs a prosperarem financeiramente.**
