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
| Build | Maven (Maven Wrapper incluso) |

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
- **Otimização financeira**: análise de excessos, sugestão de redução por categoria e recomendação geral

---

## Como rodar localmente

**Pré-requisitos:** Java 21 e Maven 3.x (ou use o Maven Wrapper incluso).

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

### Variáveis de ambiente (opcionais)

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `JWT_SECRET` | valor de exemplo | Segredo Base64 para assinar o JWT |
| `JWT_EXPIRATION` | `3600000` | Expiração do token em ms (1 hora) |

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
      "nivel": "AVISO",
      "valorLimite": 400.00,
      "valorGasto": 450.00,
      "percentual": 112.5,
      "mensagem": "Orçamento estourado em R$ 50,00"
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
      "sugestao": "Reduza gastos com ALIMENTACAO em R$ 50,00 para ficar dentro do orçamento."
    }
  ],
  "categoriasSeemOrcamento": ["LAZER"],
  "economiasPotenciais": 50.00,
  "recomendacaoGeral": "Você está comprometendo 35% da sua renda. Defina orçamentos para as categorias sem limite."
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
│   ├── usecase/                   # Um caso de uso por operação (execute())
│   │   ├── RegisterUserUseCase
│   │   ├── LoginUserUseCase
│   │   ├── GetUserProfileUseCase
│   │   ├── IngestaoTransacoesUseCase
│   │   ├── ResumoTransacoesUseCase
│   │   ├── InsightsTransacoesUseCase
│   │   ├── OrcamentoUseCase
│   │   └── OtimizacaoUseCase
│   ├── service/                   # HashService (SHA-256), CategorizacaoService
│   └── dto/                       # TransacaoRaw, ParseResult
│
├── infrastructure/                # Implementações Spring e integrações externas
│   ├── parser/                    # CsvParser, OfxParser
│   ├── persistence/               # UserRepository, TransacaoRepository, OrcamentoRepository
│   └── security/                  # JwtService, JwtAuthenticationFilter, SecurityConfig
│
└── interfaces/                    # Camada HTTP
    ├── controller/                # AuthController, TransacaoController, OrcamentoController
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

| Tipo | Cobertura |
|------|-----------|
| Testes unitários | `RegisterUserUseCase`, `LoginUserUseCase`, `IngestaoTransacoesUseCase`, `HashService`, `CategorizacaoService`, `CsvParser`, `OfxParser` |
| Testes de integração | Fluxo completo de autenticação e importação de transações com H2 + MockMvc |

---

## Time de desenvolvimento

Projeto desenvolvido durante o **Hackathon Ada Tech**, com foco em impacto social para o público das classes C e D.

| Nome | GitHub |
|------|--------|
| Meliza Maia | [@melizamaia](https://github.com/melizamaia) |
| Joyce | [@joycejsm](https://github.com/joycejsm) |
| Yasmine Oenning | [@ysmneonng](https://github.com/ysmneonng) |

---

*Desenvolvido com Java 21 + Spring Boot 3.3 · Hackathon Ada Tech 2026*
