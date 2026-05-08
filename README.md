# Extrato Popular

> API REST de gestão financeira pessoal para as classes C e D — sem burocracia, sem custo, sem exigência de conta bancária premium.

---

## O problema que resolvemos

No Brasil, **mais de 100 milhões de pessoas** das classes C e D não têm acesso às ferramentas de inteligência financeira que os bancos oferecem apenas para clientes premium. Elas pagam as mesmas tarifas, mas recebem planilhas em PDF que ninguém consegue ler.

O **Extrato Popular** transforma qualquer extrato bancário (CSV ou OFX) em um painel de controle financeiro completo: categorização automática de gastos, orçamentos por categoria, alertas de limite e sugestões de otimização — tudo via API, acessível de qualquer aplicativo ou dispositivo.

**Quem se beneficia:**
- Trabalhadores autônomos e MEIs que precisam separar despesas pessoais das profissionais
- Famílias que querem entender para onde vai o dinheiro no fim do mês
- Usuários de fintechs e bancos digitais que exportam extratos em CSV/OFX

---

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3 |
| Segurança | Spring Security 6 + JWT (JJWT 0.12) |
| Persistência | Spring Data JPA + H2 (dev) + PostgreSQL (produção) |
| Documentação | SpringDoc OpenAPI 2 (Swagger UI) |
| Testes | JUnit 5 + Mockito + MockMvc |
| IA / RAG | Spring AI 1.0.0 (`ChatClient`) + OpenAI API (gpt-4o-mini) |
| Build | Maven 3.9 (wrapper incluso) |

---

## Funcionalidades implementadas

### Autenticação e perfil
- Cadastro de usuário com e-mail e senha (BCrypt)
- Login com geração de token JWT (validade configurável)
- Recuperação do perfil do usuário autenticado

### Ingestão de transações
- Upload em lote de arquivos **CSV** e **OFX/QFX**
- Suporte a múltiplos separadores (`,` e `;`) e formatos de data (`yyyy-MM-dd` e `dd/MM/yyyy`)
- **Categorização automática** por palavras-chave na descrição
- **Deduplicação** via hash SHA-256 de `data + valor + descrição` — sem duplicatas, mesmo reimportando o mesmo arquivo
- Contagem separada de transações importadas, duplicadas e com erro de parse

### Análise financeira
- **Resumo mensal**: receitas, despesas, saldo e gastos por categoria
- **Insights**: categoria com maior gasto, percentual da renda comprometida, média por transação e top categorias
- **Orçamentos**: limite de gasto por categoria/mês/ano com CRUD completo
- **Alertas automáticos** integrados ao resumo: INFO (≥ 70%), AVISO (≥ 90%), CRÍTICO (≥ 100%)
- **Otimização financeira**: análise de excessos com 3 algoritmos intercambiáveis via **Strategy Pattern** — Knapsack (mochila), Gulosa e ROI — selecionados automaticamente conforme o perfil de gastos do usuário

### Inteligência Artificial (Pipeline RAG)
- **Chat financeiro** (`POST /chat`): assistente inteligente que responde perguntas sobre os próprios gastos do usuário em linguagem natural
- **Relatório IA** (`GET /relatorio`): relatório financeiro personalizado gerado automaticamente pela IA com base no histórico de transações
- **FinancialContextService**: constrói o contexto financeiro dos últimos 90 dias — filtra, ordena por data, limita 50 transações, calcula receitas/despesas/saldo e top 3 categorias de gasto, com limite de ~4000 caracteres
- **ChatFinanceService** e **ReportFinanceService**: use cases que orquestram o contexto + Spring AI `ChatClient` com system prompts específicos para cada finalidade
- Resposta amigável sem chamar o LLM quando não há transações nos últimos 90 dias
- **AiIntegrationException**: exceção de domínio para falhas no serviço de IA — retorna HTTP 503 sem expor detalhes internos
- Integração com **OpenAI API** (gpt-4o-mini) via **Spring AI 1.0.0** (`ChatClient`)

### Segurança e auditoria da IA
- **Isolamento multi-tenant**: `userId` é extraído exclusivamente do token JWT via `SecurityUtils.getCurrentUserId()` — nenhum endpoint aceita `userId` via body, query param ou path variable
- **Exceção unificada**: toda falha do LLM lança `AiIntegrationException` (com causa encadeada) e retorna `503 Service Unavailable` sem expor detalhes internos
- **Timeout configurável** (`openai.timeout-ms`, padrão 10s): evita que lentidão da OpenAI trave requisições indefinidamente
- **Tratamento de falhas da IA**: erros de rede, timeout e respostas inválidas retornam `503 Service Unavailable` com mensagem clara, sem expor detalhes internos
- **Testes de vazamento**: suite de integração que prova o isolamento entre usuários interceptando o prompt real enviado à IA

---

## Como rodar localmente

**Pré-requisitos:** Java 21. Maven Wrapper incluso — não é necessário instalar Maven.

```bash
# 1. Clonar o repositório
git clone https://github.com/melizamaia/Hackathon-Ada-Extrato-Popular.git
cd Hackathon-Ada-Extrato-Popular

# 2. Rodar (H2 in-memory, sem necessidade de banco externo)
./mvnw spring-boot:run

# 3. Executar os testes
./mvnw test
```

A aplicação sobe na porta **8080**.

| Interface | URL |
|-----------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |

> **H2 Console:** JDBC URL `jdbc:h2:mem:extratodb` · Usuário: `sa` · Senha: *(vazia)*

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `OPENAI_API_KEY` | `chave-nao-configurada` | Chave da API OpenAI (obrigatória para chat e relatório) |
| `JWT_SECRET` | valor Base64 de exemplo | Segredo Base64 para assinar o JWT |
| `JWT_EXPIRATION` | `3600000` | Expiração do token em ms (1 hora) |
| `openai.timeout-ms` | `10000` | Timeout das chamadas à OpenAI em ms (padrão 10s) |

Para usar PostgreSQL em produção, descomente o bloco correspondente em `src/main/resources/application.properties` e ajuste as credenciais.

---

## Endpoints

Todas as rotas protegidas exigem o header:
```
Authorization: Bearer <token>
```

### Autenticação

#### `POST /auth/register` — Cadastro de usuário

**Request:**
```json
{
  "nome": "Maria Silva",
  "email": "maria.silva@email.com",
  "senha": "minhasenha123",
  "rendaMensal": 3500.00
}
```

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "maria.silva@email.com"
}
```

---

#### `POST /auth/login` — Login

**Request:**
```json
{
  "email": "maria.silva@email.com",
  "senha": "minhasenha123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "maria.silva@email.com"
}
```

---

#### `GET /auth/me` — Perfil do usuário autenticado

**Response `200 OK`:**
```json
{
  "id": 1,
  "nome": "Maria Silva",
  "email": "maria.silva@email.com",
  "rendaMensal": 3500.00
}
```

---

### Transações

#### `POST /transacoes/bulk` — Importação em lote `🔒`

Envie um arquivo `.csv`, `.ofx` ou `.qfx` via `multipart/form-data`.

```bash
curl -X POST http://localhost:8080/transacoes/bulk \
  -H "Authorization: Bearer <token>" \
  -F "file=@extrato-maio.csv"
```

**Formato CSV aceito:**
```csv
data,valor,descricao
2026-05-01,-150.00,IFOOD RESTAURANTE
2026-05-02,3500.00,SALARIO MAIO
2026-05-03,-80.00,UBER VIAGEM
2026-05-04,-200.00,FARMACIA DROGASIL
```

**Response `201 Created`:**
```json
{
  "importadas": 4,
  "duplicatas": 0,
  "erros": 0,
  "transacoes": [
    {
      "id": 1,
      "data": "2026-05-01",
      "valor": -150.00,
      "descricao": "IFOOD RESTAURANTE",
      "categoria": "ALIMENTACAO",
      "tipo": "DEBITO",
      "fonte": "CSV"
    }
  ]
}
```

> - Separadores aceitos: `,` ou `;`
> - Formatos de data: `yyyy-MM-dd` ou `dd/MM/yyyy`
> - Valores negativos = débito · positivos = crédito
> - Linhas em branco e comentários (`#`) são ignorados

---

#### `GET /transacoes/resumo?mes=5&ano=2026` — Resumo mensal `🔒`

**Response `200 OK`:**
```json
{
  "mes": 5,
  "ano": 2026,
  "totalReceitas": 3500.00,
  "totalDespesas": 1240.00,
  "saldo": 2260.00,
  "totalTransacoes": 12,
  "gastosPorCategoria": {
    "ALIMENTACAO": 450.00,
    "TRANSPORTE": 280.00,
    "SAUDE": 200.00,
    "LAZER": 150.00,
    "MORADIA": 160.00
  },
  "alertas": [
    {
      "categoria": "ALIMENTACAO",
      "nivel": "CRITICO",
      "valorLimite": 400.00,
      "valorGasto": 450.00,
      "percentual": 112.50,
      "mensagem": "Orçamento de ALIMENTACAO estourou! 112.50% utilizado (R$ 450.00 de R$ 400.00)"
    }
  ]
}
```

---

#### `GET /transacoes/insights` — Insights financeiros `🔒`

**Response `200 OK`:**
```json
{
  "categoriaComMaiorGasto": "ALIMENTACAO",
  "percentualRendaComprometida": 35.43,
  "mediaGastoPorTransacao": 113.64,
  "alertaGastoElevado": false,
  "topCategorias": [
    { "categoria": "ALIMENTACAO", "total": 450.00, "percentualDoTotal": 36.29 },
    { "categoria": "TRANSPORTE",  "total": 280.00, "percentualDoTotal": 22.58 },
    { "categoria": "SAUDE",       "total": 200.00, "percentualDoTotal": 16.13 }
  ]
}
```

---

### Orçamentos

#### `POST /orcamentos` — Criar orçamento `🔒`

**Request:**
```json
{
  "categoria": "ALIMENTACAO",
  "valorLimite": 400.00,
  "mes": 5,
  "ano": 2026
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "categoria": "ALIMENTACAO",
  "valorLimite": 400.00,
  "mes": 5,
  "ano": 2026
}
```

---

#### `GET /orcamentos?mes=5&ano=2026` — Listar orçamentos do mês `🔒`

**Response `200 OK`:**
```json
[
  { "id": 1, "categoria": "ALIMENTACAO", "valorLimite": 400.00, "mes": 5, "ano": 2026 },
  { "id": 2, "categoria": "TRANSPORTE",  "valorLimite": 300.00, "mes": 5, "ano": 2026 }
]
```

---

#### `PUT /orcamentos/{id}` — Atualizar orçamento `🔒`

Mesmo body do `POST /orcamentos`. Retorna `200 OK` com o orçamento atualizado.

---

#### `DELETE /orcamentos/{id}` — Remover orçamento `🔒`

Retorna `204 No Content`.

---

#### `GET /orcamentos/otimizacao?mes=5&ano=2026` — Otimização financeira `🔒`

**Response `200 OK`:**
```json
{
  "mes": 5,
  "ano": 2026,
  "saldoMensal": 2260.00,
  "percentualRendaComprometida": 35.43,
  "gastosAcimaOrcamento": [
    {
      "categoria": "ALIMENTACAO",
      "valorOrcamento": 400.00,
      "valorGasto": 450.00,
      "excesso": 50.00,
      "sugestao": "Reduza R$ 50.00 em ALIMENTACAO para ficar dentro do orçamento"
    }
  ],
  "categoriasSeemOrcamento": ["LAZER"],
  "economiasPotenciais": 50.00,
  "recomendacaoGeral": "Você tem 1 categoria(s) acima do orçamento. Economias potenciais de R$ 50.00 se os limites fossem respeitados."
}
```

---

### Inteligência Artificial

#### `POST /chat` — Chat com assistente financeiro `🔒`

O assistente recebe a pergunta e responde com base nas transações reais do usuário (pipeline RAG). O `userId` é extraído do token JWT — não deve ser enviado no body.

**Request:**
```json
{
  "pergunta": "Quanto gastei com alimentação esse mês e o que posso cortar?"
}
```

**Response `200 OK`:**
```json
{
  "resposta": "No mês de maio você gastou R$ 450,00 com alimentação, acima do seu orçamento de R$ 400,00. Os maiores gastos foram com iFood (R$ 210,00) e supermercado (R$ 180,00). Uma sugestão é reduzir os pedidos por aplicativo para no máximo 3 vezes por semana."
}
```

---

#### `GET /relatorio` — Relatório financeiro gerado por IA `🔒`

Gera um relatório textual completo e personalizado com base em todo o histórico de transações do usuário.

**Response `200 OK`:**
```json
{
  "relatorio": "Relatório Financeiro — Maria Silva\n\nSeu saldo do mês foi positivo em R$ 2.260,00. O maior gasto foi em Alimentação (36% do total de despesas). Você estourou o orçamento de Alimentação em R$ 50,00 e ainda não definiu limites para Lazer. Recomenda-se criar um orçamento de no máximo R$ 200,00 para essa categoria. No geral, sua saúde financeira está estável."
}
```

---

#### `POST /api/v1/chat` — Chat IA (v2) `🔒`

Versão aprimorada do chat com contexto RAG e isolamento multi-tenant garantido.

**Request:**
```json
{ "message": "Como estão meus gastos este mês?" }
```

**Response `200 OK`:**
```json
{ "response": "Nos últimos 90 dias você teve R$ 1.240,00 em despesas..." }
```

---

#### `GET /api/v1/relatorio` — Relatório IA (v2) `🔒`

Versão aprimorada do relatório com avaliação de saúde financeira (Positiva / Atenção / Crítica).

**Response `200 OK`:**
```json
{
  "relatorio": "## Relatório Financeiro\n\n**Saúde financeira: Positiva**\n\nPeríodo analisado: ..."
}
```

---

### Alertas de orçamento

| Nível | Percentual atingido | Significado |
|-------|---------------------|-------------|
| `INFO` | ≥ 70% | Atenção: aproximando do limite |
| `AVISO` | ≥ 90% | Cuidado: quase no limite |
| `CRITICO` | ≥ 100% | Orçamento estourado |

---

### Categorização automática

| Categoria | Palavras-chave reconhecidas |
|-----------|-----------------------------|
| `ALIMENTACAO` | ifood, restaurante, mercado, supermercado, padaria, açougue |
| `TRANSPORTE` | uber, 99, onibus, metro, combustivel, posto, estacionamento |
| `SAUDE` | farmacia, hospital, clinica, laboratorio, drogaria |
| `EDUCACAO` | escola, faculdade, curso, livraria, mensalidade |
| `LAZER` | netflix, spotify, cinema, lazer, teatro, streaming |
| `MORADIA` | aluguel, condominio, luz, agua, gas, internet, telefone |
| `SALARIO` | salario, pagamento folha, renda |
| `TRANSFERENCIA` | transferencia, pix, ted, doc |
| `OUTROS` | *(fallback para descrições não identificadas)* |

---

### Códigos de erro

Todos os erros seguem o padrão:
```json
{ "erro": "CODIGO_DO_ERRO", "detalhes": ["mensagem descritiva"] }
```

| Código | HTTP | Situação |
|--------|------|----------|
| `VALIDACAO_FALHOU` | 400 | Campo obrigatório ausente ou inválido |
| `ARQUIVO_VAZIO` | 400 | Arquivo sem transações válidas |
| `FORMATO_INVALIDO` | 400 | Extensão de arquivo não suportada |
| `CREDENCIAIS_INVALIDAS` | 401 | E-mail ou senha incorretos |
| `NAO_AUTENTICADO` | 401 | Token ausente ou expirado |
| `EMAIL_JA_CADASTRADO` | 409 | E-mail já em uso |
| `TRANSACAO_DUPLICADA` | 409 | Hash já existe para o usuário |
| `ORCAMENTO_DUPLICADO` | 409 | Já existe orçamento para esta categoria/mês/ano |
| `ORCAMENTO_NAO_ENCONTRADO` | 404 | Orçamento não encontrado |
| `AI_INDISPONIVEL` | 503 | Serviço de IA temporariamente indisponível |
| `ERRO_INTERNO` | 500 | Erro inesperado no servidor |

---

## Estrutura do projeto

O projeto segue **Clean Architecture** com separação estrita de responsabilidades:

```
src/main/java/com/extratoPopular/
│
├── domain/                        # Regras de negócio puras (sem Spring)
│   ├── model/                     # Entidades JPA: User, Transacao, Orcamento
│   ├── enums/                     # Categoria, TipoTransacao, FonteImportacao, NivelAlerta
│   └── exception/                 # Exceções tipadas de domínio
│
├── application/                   # Casos de uso e serviços de aplicação
│   ├── usecase/                   # Um caso de uso por operação
│   │   ├── RegisterUserUseCase
│   │   ├── LoginUserUseCase
│   │   ├── GetUserProfileUseCase
│   │   ├── IngestaoTransacoesUseCase
│   │   ├── ResumoTransacoesUseCase
│   │   ├── InsightsTransacoesUseCase
│   │   ├── OrcamentoUseCase
│   │   ├── OtimizacaoUseCase
│   │   ├── ChatFinanceService      # Orquestra contexto + Spring AI para o chat
│   │   └── ReportFinanceService    # Orquestra contexto + Spring AI para o relatório
│   ├── service/                   # HashService, CategorizacaoService
│   │   ├── FinancialContextService # Interface: buildContext(Long userId)
│   │   ├── ChatService            # Orquestra RAG + OpenAI para o chat (v1)
│   │   ├── RelatorioService       # Orquestra RAG + OpenAI para o relatório (v1)
│   │   ├── otimizacao/            # Strategy Pattern: OtimizacaoStrategy (interface),
│   │   │                          # KnapsackOtimizacaoStrategy, GulosaOtimizacaoStrategy,
│   │   │                          # RoiOtimizacaoStrategy, OtimizacaoStrategyFactory
│   │   └── rag/                   # ContextoFinanceiroService, PromptFinanceiroService,
│   │                              # PromptRelatorioService
│   └── dto/                       # TransacaoRaw, ParseResult
│
├── infrastructure/                # Implementações Spring e integrações externas
│   ├── ai/                        # OpenAiClient, FinancialContextServiceImpl
│   │                              # (contexto 90 dias, top 3 categorias, limite 4000 chars)
│   ├── parser/                    # CsvParser, OfxParser
│   ├── persistence/               # UserRepository, TransacaoRepository, OrcamentoRepository
│   ├── security/                  # JwtService, JwtAuthenticationFilter, SecurityConfig, SecurityUtils
│   └── config/                    # SwaggerConfig, AiConfig (bean ChatClient Spring AI)
│
└── interfaces/                    # Camada HTTP
    ├── controller/                # AuthController, TransacaoController, OrcamentoController,
    │                              # ChatController, RelatorioController, AiController (v2)
    ├── dto/                       # Records de request e response HTTP
    └── handler/                   # GlobalExceptionHandler
```

---

## Testes

```bash
# Todos os testes
./mvnw test

# Uma classe específica
./mvnw test -Dtest=AuthControllerIntegrationTest

# Um método específico
./mvnw test -Dtest=AuthControllerIntegrationTest#deve_retornar201_quando_registrarComDadosValidos
```

**234 testes · 0 falhas · BUILD SUCCESS**

| Classe de teste | Testes | Tipo |
|----------------|--------|------|
| `CategorizacaoServiceTest` | 52 | Unitário |
| `CsvParserTest` | 16 | Unitário |
| `OfxParserTest` | 15 | Unitário |
| `OrcamentoControllerIntegrationTest` | 13 | Integração |
| `FinancialContextServiceImplTest` | 12 | Unitário |
| `IngestaoTransacoesUseCaseTest` | 12 | Unitário |
| `OrcamentoUseCaseTest` | 11 | Unitário |
| `ResumoTransacoesUseCaseTest` | 11 | Unitário |
| `OtimizacaoUseCaseTest` | 10 | Unitário |
| `AnaliseFinanceiraControllerIntegrationTest` | 9 | Integração |
| `AuthControllerIntegrationTest` | 8 | Integração |
| `ReportFinanceServiceTest` | 8 | Unitário |
| `ChatMultiTenantIntegrationTest` | 7 | Integração |
| `ChatFinanceServiceTest` | 7 | Unitário |
| `ChatRelatorioControllerIntegrationTest` | 7 | Integração |
| `HashServiceTest` | 6 | Unitário |
| `TransacaoControllerIntegrationTest` | 6 | Integração |
| `AiControllerIntegrationTest` | 5 | Integração |
| `ChatServiceTest` | 5 | Unitário |
| `RelatorioServiceTest` | 5 | Unitário |
| `AiIntegrationExceptionTest` | 4 | Unitário |
| `LoginUserUseCaseTest` | 3 | Unitário |
| `RegisterUserUseCaseTest` | 2 | Unitário |

---

## Time de desenvolvimento

Projeto desenvolvido durante o **Hackathon Ada Tech**, com foco em impacto social para o público das classes C e D.

| Nome | Responsabilidade | GitHub |
|------|-----------------|--------|
| Joyce Silva | Autenticação e Segurança (JWT) | [@joycejsm](https://github.com/joycejsm) |
| Meliza Maia | Ingestão de Dados e Parsers CSV/OFX | [@melizamaia](https://github.com/melizamaia) |
| Mellyssa Mendes | Pipeline RAG e IA | [@mellyssamnds](https://github.com/mellyssamnds) |
| Yasmine Oenning | Motor Financeiro e Algoritmos | [@ysmneonng](https://github.com/ysmneonng) |

---

*Desenvolvido com Java 21 + Spring Boot 3.3 · Hackathon Ada Tech 2026*
