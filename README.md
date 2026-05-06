# Extrato Popular

API REST de análise financeira para as classes C e D. Permite ingestão de transações via CSV/OFX, categorização automática, controle de orçamento por categoria com alertas e algoritmo de otimização financeira. Desenvolvido em Java 21 + Spring Boot 3.3 seguindo Clean Architecture.

## Tecnologias

- **Java 21** · **Spring Boot 3.3**
- **Spring Security** + **JWT** (JJWT 0.12)
- **Spring Data JPA** · **H2** (dev/test) · **PostgreSQL** (produção)
- **SpringDoc OpenAPI** (Swagger UI)
- **Lombok** · **JUnit 5** · **Mockito**

## Funcionalidades implementadas

| Módulo | Endpoint | Descrição |
|--------|----------|-----------|
| Autenticação | `POST /auth/register` | Cadastro de usuário |
| Autenticação | `POST /auth/login` | Login, retorna JWT |
| Autenticação | `GET /auth/me` | Perfil do usuário autenticado |
| Ingestão | `POST /transacoes/bulk` | Upload em lote (CSV ou OFX/QFX) |
| Resumo | `GET /transacoes/resumo` | Resumo financeiro mensal com alertas de orçamento |
| Insights | `GET /transacoes/insights` | Top categorias, % renda comprometida, média por transação |
| Orçamentos | `POST /orcamentos` | Criar orçamento por categoria/mês/ano |
| Orçamentos | `GET /orcamentos` | Listar orçamentos do mês |
| Orçamentos | `PUT /orcamentos/{id}` | Atualizar orçamento |
| Orçamentos | `DELETE /orcamentos/{id}` | Remover orçamento |
| Otimização | `GET /orcamentos/otimizacao` | Sugestões de redução de gastos |

## Rodando localmente

**Pré-requisitos:** Java 21, Maven 3.x

> Se o seu `java -version` mostrar outra versão, defina o JAVA_HOME antes de rodar:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

```bash
# clonar
git clone https://github.com/melizamaia/Hackathon-Ada-Extrato-Popular.git
cd Hackathon-Ada-Extrato-Popular

# rodar (usa H2 in-memory, sem setup de banco)
mvn spring-boot:run
```

Acesse a documentação interativa em `http://localhost:8080/swagger-ui.html`.

O console do H2 está disponível em `http://localhost:8080/h2-console`  
(JDBC URL: `jdbc:h2:mem:extratodb`, usuário: `sa`, senha: vazia).

## Configuração

Todas as configurações ficam em `src/main/resources/application.properties`.

| Propriedade | Padrão | Descrição |
|-------------|--------|-----------|
| `jwt.secret` | valor de exemplo | Segredo Base64 para assinar o JWT |
| `jwt.expiration` | `3600000` | Expiração do token em ms (1 hora) |

Para usar PostgreSQL em produção, descomente o bloco comentado no `application.properties` e ajuste as credenciais.

## Executando os testes

```bash
mvn test
```

## Ingestão de transações

`POST /transacoes/bulk` aceita arquivos `.csv` ou `.ofx`/`.qfx` via `multipart/form-data`. Requer token Bearer.

**Formato CSV esperado:**

```csv
data,valor,descricao
2024-06-01,-150.00,IFOOD RESTAURANTE
2024-06-02,3000.00,SALARIO JUNHO
```

- Separador: `,` ou `;`
- Data: `yyyy-MM-dd` ou `dd/MM/yyyy`
- Valores negativos = débito · positivos = crédito
- Linhas em branco e comentários (`#`) são ignorados
- Deduplicação automática por hash SHA-256 de `data + valor + descrição`

## Orçamentos e alertas

Defina um limite de gasto por categoria e mês:

```bash
POST /orcamentos
{
  "categoria": "ALIMENTACAO",
  "valorLimite": 800.00,
  "mes": 5,
  "ano": 2026
}
```

O `GET /transacoes/resumo?mes=5&ano=2026` retorna automaticamente os alertas:

| Percentual atingido | Nível | Descrição |
|---------------------|-------|-----------|
| ≥ 70% | `INFO` | Alerta informativo |
| ≥ 90% | `AVISO` | Aproximando do limite |
| ≥ 100% | `CRITICO` | Orçamento estourado |

## Otimização financeira

`GET /orcamentos/otimizacao?mes=5&ano=2026` analisa os gastos do mês e retorna:

- Categorias com orçamento estourado e valor do excesso
- Sugestão de redução por categoria
- Categorias com gastos mas sem orçamento definido
- Percentual da renda mensal comprometida
- Recomendação geral

## Categorias automáticas

| Categoria | Palavras-chave |
|-----------|---------------|
| `ALIMENTACAO` | ifood, restaurante, mercado, supermercado, padaria, açougue |
| `TRANSPORTE` | uber, 99, onibus, metro, combustivel, posto, estacionamento |
| `SAUDE` | farmacia, hospital, clinica, laboratorio, drogaria |
| `EDUCACAO` | escola, faculdade, curso, livraria, mensalidade |
| `LAZER` | netflix, spotify, cinema, lazer, teatro, streaming |
| `MORADIA` | aluguel, condominio, luz, agua, gas, internet, telefone |
| `SALARIO` | salario, pagamento folha, renda |
| `TRANSFERENCIA` | transferencia, pix, ted, doc |
| `OUTROS` | *(fallback)* |

## Estrutura do projeto

```
src/main/java/com/extratoPopular/
├── domain/
│   ├── model/          # Entidades JPA (User, Transacao, Orcamento)
│   ├── enums/          # Categoria, TipoTransacao, FonteImportacao, NivelAlerta
│   └── exception/      # Exceções de domínio
├── application/
│   ├── usecase/        # Casos de uso (Register, Login, Ingestao, Resumo, Insights, Orcamento, Otimizacao)
│   ├── service/        # HashService, CategorizacaoService
│   └── dto/            # TransacaoRaw, ParseResult
├── infrastructure/
│   ├── parser/         # CsvParser, OfxParser
│   ├── persistence/    # TransacaoRepository, UserRepository, OrcamentoRepository
│   └── security/       # JWT, filtros, SecurityConfig
└── interfaces/
    ├── controller/     # AuthController, TransacaoController, OrcamentoController
    ├── dto/            # Requests e Responses HTTP
    └── handler/        # GlobalExceptionHandler
```

## Códigos de erro

| Código | HTTP | Situação |
|--------|------|----------|
| `VALIDACAO_FALHOU` | 400 | Campo obrigatório ausente ou inválido |
| `ARQUIVO_VAZIO` | 400 | Arquivo sem transações válidas |
| `FORMATO_INVALIDO` | 400 | Extensão não suportada |
| `CREDENCIAIS_INVALIDAS` | 401 | E-mail ou senha incorretos |
| `NAO_AUTENTICADO` | 401 | Token ausente ou expirado |
| `EMAIL_JA_CADASTRADO` | 409 | E-mail já em uso |
| `TRANSACAO_DUPLICADA` | 409 | Hash já existe para o usuário |
| `ORCAMENTO_DUPLICADO` | 409 | Já existe orçamento para esta categoria/mês/ano |
| `ORCAMENTO_NAO_ENCONTRADO` | 404 | Orçamento não encontrado |
| `ERRO_INTERNO` | 500 | Erro inesperado no servidor |
