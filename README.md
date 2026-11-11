<div align="center">
  <h1>Humanizar - Acolhimento (Microservice)</h1>
  <p>Gestão de acolhimento, anamnese técnica, alocação de equipe técnica por núcleo (staff) e vínculo de avaliações do paciente em Rust, com API interna protegida, PostgreSQL assíncrono e mensageria RabbitMQ.</p>

  <img alt="Rust" src="https://img.shields.io/badge/Rust-1.99-000000?style=for-the-badge&logo=rust&logoColor=white" />
  <img alt="Axum" src="https://img.shields.io/badge/Axum-0.8-7B1FA2?style=for-the-badge&logo=rust&logoColor=white" />
  <img alt="Tokio" src="https://img.shields.io/badge/Tokio-1.53-2C5BB4?style=for-the-badge&logo=rust&logoColor=white" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img alt="RabbitMQ" src="https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white" />
  <img alt="Docker" src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
  <img alt="Keycloak" src="https://img.shields.io/badge/Keycloak-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white" />
</div>

<br/>

O `humanizar-acolhimento` é o serviço central do ecossistema Humanizar responsável pelo ciclo de acolhimento, registro e acompanhamento do paciente. Migrado integralmente de Java para Rust (executando na porta `9099`), ele absorveu todas as responsabilidades que pertenciam ao extinto serviço `humanizar-nucleo-relacionamento`, unificando a gestão de núcleos e equipe técnica do paciente.

O serviço opera com runtime assíncrono Axum e Tokio, persistência PostgreSQL com pool BB8 e composição explícita baseada em Arquitetura Hexagonal. Os eventos de alocação de equipe técnica são registrados em Outbox para publicação no RabbitMQ e processamento pelo `humanizar-notification`.

## Responsabilidades

- Registro, consulta, atualização e exclusão do acolhimento do paciente;
- Gestão do relatório técnico e anamnese estruturada por núcleo (`nucleo_patient`);
- Alocação e manutenção da equipe técnica por núcleo (`nucleo_patient_staff`), identificando o colaborador exclusivamente pelo campo declarativo `staff_user_id` e seu papel (`role`);
- Vinculação, acompanhamento e desassociação de avaliações clínicas e questionários (`acolhimento_avaliacao`) integrados com o `humanizar-avaliacao-paciente`;
- Publicação de eventos transacionais de equipe (`staff.vinculado` e `staff.desvinculado`) direcionados à `humanizar-notification`;
- Atendimento assíncrono a comandos de consulta de acolhimento via RabbitMQ, com deduplicação por identidade e conteúdo do evento;
- Rastreabilidade ponta a ponta através do cabeçalho `X-Correlation-ID`.

## Arquitetura e Padrões

O projeto organiza suas responsabilidades em camadas de Arquitetura Hexagonal:

- **Camadas estritas**: `domain`, `application` e `infrastructure`, com dependências sempre apontando para o domínio;
- **Centralização de módulos**: todos os módulos registrados explicitamente em `src/lib.rs`, **sem nenhum arquivo `mod.rs`**;
- **Portas e Adaptadores**: portas assíncronas `Send + Sync` injetadas como `Arc<dyn Port>`;
- **Isolamento de DTOs**: DTOs separados para HTTP e Mensageria com mappers manuais campo a campo e métodos construtores/getters privados;
- **Persistência explícita**: repositórios responsáveis por SQL parametrizado nativo em caixa alta (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `FROM`, `WHERE`);
- **Pool de Conexões**: PostgreSQL assíncrono com BB8 e validação de conectividade no checkout;
- **Tratamento de Erros**: enum `ReasonCode` estável, exceções tipadas de domínio e handlers padronizados na infraestrutura;
- **Transactional Outbox & Idempotência**: schema canônico de 21 campos em `outbox_events` e 16 campos em `processed_events`, com workers de relay e retenção supervisionados;
- **Código Seguro**: `#![forbid(unsafe_code)]` em todo o código da aplicação.

## Regras de Domínio

### Acolhimento
- Vínculo exclusivo 1:1 com o paciente (`patient_id`), registrando data da anamnese, horário e observações gerais.
- Atualizações preservam a identidade original e a rastreabilidade do registro.

### Núcleo do Paciente
- Um paciente pode ser acolhido em múltiplos núcleos de atendimento (`nucleo_patient`).
- Cada núcleo armazena sua anamnese técnica estruturada em formato JSONB (`anamnese`).
- Vínculos entre paciente e núcleos são únicos (`uk_nucleo_patient_patient_nucleo`).

### Equipe Técnica do Núcleo (Staff)
- Os colaboradores alocados ao paciente em cada núcleo são registrados em `nucleo_patient_staff`.
- O identificador do colaborador é estritamente **`staff_user_id`** (UUID), acompanhado de seu papel funcional (`role`).
- A nomenclatura legada `responsavel_id` foi completamente eliminada, sem suporte a aliases.
- A criação ou remoção de membros na equipe gera eventos outbox transacionais para notificação imediata.

### Avaliações Vinculadas
- As avaliações clínicas criadas em `humanizar-avaliacao-paciente` podem ser vinculadas ao acolhimento através de `acolhimento_avaliacao`.
- Armazena `patient_id`, `avaliacao_paciente_id`, `titulo_avaliacao`, `modalidade` e `status`.
- Permite listagem unificada, registro de novos vínculos e exclusão/desassociação.

## API HTTP

Base path: `/api/v1/acolhimento`. Porta padrão: `9099`.

### Verificação de Saúde

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| `GET` | `/health` | Pública | Retorna `200 {"status":"UP"}` se o PostgreSQL e o RabbitMQ responderem no prazo. Retorna `503` em caso de indisponibilidade. |

### Consultas (Exigem Bearer JWT)

| Método | Rota | Descrição |
| --- | --- | --- |
| `GET` | `/api/v1/acolhimento/paciente/{patientId}` | Retorna o acolhimento completo do paciente com núcleos e staff |
| `GET` | `/api/v1/acolhimento/paciente/status/{patientId}` | Retorna o status resumido de acolhimento do paciente |
| `GET` | `/api/v1/acolhimento/anamnese/paciente/{patientId}` | Retorna a anamnese estruturada dos núcleos do paciente |
| `GET` | `/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}` | Retorna a anamnese do núcleo informado |
| `GET` | `/api/v1/acolhimento/avaliacoes/{patientId}` | Lista todas as avaliações clínicas vinculadas ao paciente |
| `GET` | `/api/v1/acolhimento/avaliacoes-disponiveis/{patientId}` | Consulta avaliações disponíveis na fonte canônica |

### Comandos e Mutações (Exigem Bearer JWT + Role `ADMINISTRADOR`)

As mutações exigem um `Idempotency-Key` UUID por intenção. Após perda de resposta, consulte `GET /api/v1/acolhimento/operacoes/{operationId}?acao=...` com o mesmo usuário antes de uma nova tentativa.

| Método | Rota | Código HTTP | Descrição |
| --- | --- | --- | --- |
| `POST` | `/api/v1/acolhimento/register` | `201 Created` | Cria o registro de acolhimento do paciente com núcleos e staff |
| `PUT` | `/api/v1/acolhimento/update/{id}` | `200 OK` | Atualiza o acolhimento, núcleos e equipe técnica do paciente |
| `DELETE` | `/api/v1/acolhimento/delete/{patientId}` | `200 OK` | Remove o registro de acolhimento e desassocia núcleos do paciente |
| `POST` | `/api/v1/acolhimento/anamnese/register` | `200 OK` | Registra ou atualiza a anamnese geral do paciente |
| `PUT` | `/api/v1/acolhimento/anamnese/update/{patientId}` | `200 OK` | Atualiza a anamnese técnica do paciente |
| `POST` | `/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}` | `201 Created` | Registra a anamnese do núcleo |
| `PUT` | `/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}` | `200 OK` | Atualiza a anamnese do núcleo |
| `DELETE` | `/api/v1/acolhimento/anamnese/{patientId}/{nucleoId}` | `200 OK` | Remove apenas a anamnese do núcleo informado |
| `POST` | `/api/v1/acolhimento/avaliacoes/register` | `201 Created` | Vincula uma avaliação existente ao acolhimento do paciente |
| `DELETE` | `/api/v1/acolhimento/avaliacoes/{id}` | `200 OK` | Desvincula uma avaliação de acolhimento pelo ID |

### Padrão de Respostas HTTP

Respostas bem-sucedidas utilizam o envelope unificado `SuccessResponseDto`:

```json
{
  "code": "ACOLHIMENTO_REGISTERED",
  "success": true,
  "correlationId": "4a7f9c2e-8d3b-41f2-9856-2e1189c4501a",
  "message": "Registro de acolhimento criado com sucesso.",
  "data": { ... },
  "timestamp": "2026-10-27T14:30:00Z"
}
```

Respostas de erro utilizam o envelope padronizado `ErrorResponseDto`:

```json
{
  "timestamp": "2026-10-27T14:30:00Z",
  "status": 404,
  "error": "Not Found",
  "reasonCode": "ACOLHIMENTO_NOT_FOUND",
  "message": "Acolhimento nao encontrado",
  "path": "/api/v1/acolhimento/paciente/...",
  "correlationId": "4a7f9c2e-8d3b-41f2-9856-2e1189c4501a"
}
```

### ReasonCodes Estáveis

| Status | Exemplos de ReasonCode |
| --- | --- |
| `400` | `PAYLOAD_REQUIRED`, `REQUEST_BODY_INVALID`, `REQUEST_PARAMETER_MISSING`, `REQUEST_PARAMETER_INVALID`, `VALIDATION_ERROR`, `PATIENT_ID_REQUIRED`, `NUCLEO_PATIENT_EMPTY`, `NUCLEO_STAFF_EMPTY`, `STAFF_USER_ID_REQUIRED`, `ROLE_REQUIRED`, `AVALIACAO_PACIENTE_ID_REQUIRED`, `TITULO_AVALIACAO_REQUIRED` |
| `401` | `AUTHENTICATION_REQUIRED` |
| `403` | `AUTHORIZATION_DENIED` |
| `404` | `ACOLHIMENTO_NOT_FOUND`, `NUCLEO_PATIENT_NOT_FOUND`, `ACOLHIMENTO_AVALIACAO_NOT_FOUND`, `ROUTE_NOT_FOUND` |
| `405` | `METHOD_NOT_ALLOWED` |
| `409` | `ACOLHIMENTO_ALREADY_EXISTS`, `ACOLHIMENTO_AVALIACAO_ALREADY_EXISTS`, `DUPLICATE_EVENT` |
| `413` | `PAYLOAD_TOO_LARGE` |
| `500` | `DATABASE_ACCESS_FAILED`, `UNEXPECTED_ERROR` |
| `502` | `INTEGRATION_FAILURE` |
| `503` | `PERSISTENCE_FAILURE`, `MESSAGING_UNAVAILABLE` |
| `504` | `REQUEST_TIMEOUT` |

## 🔐 Segurança

- **Autenticação Stateless**: Bearer JWT com validação criptográfica obrigatória `RS256`;
- **Validação de Claims**: verificação rigorosa de `kid`, assinatura digital, `exp`, `nbf`, `iss`, `aud` e `sub`;
- **Controle de Acesso**: autorização baseada exclusivamente em `realm_access.roles`, exigindo a role `ADMINISTRADOR` para todas as mutações (`POST`, `PUT`, `DELETE`);
- **Carregamento de Chaves (JWKS)**: chaves públicas carregadas diretamente da URL completa em `KEYCLOAK_ISSUER`, com cache em memória (TTL padrão 300s) e atualização assíncrona;
- **Proteção de Dados**: tokens, credenciais e payloads brutos de exceções são omitidos dos logs.

## ⛓️‍💥 Resiliência e Observabilidade

- **Timeouts**: timeout de requisição HTTP configurado para 30 segundos; timeouts do pool PostgreSQL ajustados para `statement_timeout=25000` e `lock_timeout=10000` ms;
- **Limitação de Carga**: limite de payload HTTP fixado em 2 MiB via `DefaultBodyLimit`;
- **Rastreabilidade**: propagação e auditoria contínua do header `X-Correlation-ID` em logs, respostas HTTP, mensagens e outbox;
- **Supervisão de Tarefas**: encerramento gracioso (graceful shutdown) via `SIGINT` / `SIGTERM` aguardando a finalização dos lotes de mensageria em andamento.

## Mensageria RabbitMQ

### Topologia de Exchanges

```text
humanizar.acolhimento.command (direct)
humanizar.acolhimento.event   (topic)
humanizar.acolhimento.dlx     (fanout)
```

### Comandos Consumidos

```text
cmd.acolhimento.patient.retrieve.v1
```

A fila associada `humanizar.acolhimento.patient.retrieve` é configurada como Quorum Queue com ACK manual e envio automático para DLQ (`humanizar.acolhimento.patient.retrieve.dlq`) após limite de tentativas.

O consumidor da DLQ mantém a entrega sem ACK enquanto tenta gravá-la em `dead_letter_quarantine`. A quarentena preserva o corpo e as propriedades da mensagem para investigação; o ACK ocorre após a gravação. É necessário aplicar a migração da quarentena antes de iniciar esta versão.

### Callbacks Publicados

```text
ev.acolhimento.patient.retrieved.v1
ev.acolhimento.patient.failed.v1
```

### Eventos de Notificação de Equipe (Consumidos por `humanizar-notification`)

```text
ev.acolhimento.staff.vinculado.v1
ev.acolhimento.staff.desvinculado.v1
```

Os eventos transportam `staffUserId`, `patientId` e `nucleoId`; o evento de vínculo também informa `role`. A entrega das notificações depende do processamento e do resultado dos provedores no `humanizar-notification`.

### Transactional Outbox e Retenção

1. Mutações de acolhimento gravam o estado de domínio e os eventos de outbox atomicamente na mesma transação PostgreSQL;
2. O worker de relay seleciona lotes com `FOR UPDATE SKIP LOCKED` e controla a posse com `locked_by` e `next_retry_at`;
3. A publicação no RabbitMQ é executada com mandatory e publisher confirms habilitados;
4. Falhas transitórias aplicam backoff exponencial até o esgotamento de tentativas (`DEAD`);
5. O worker de retenção limpa periodicamente registros `PUBLISHED` com mais de 48 horas e recibos processados com mais de 90 dias. Os registros `DEAD` permanecem disponíveis para investigação.

## Persistência PostgreSQL

Os scripts transacionais são aplicados em ordem, sem execução automática no startup:

| Arquivo | Descrição |
| --- | --- |
| [`migrations/0001_initial_schema.sql`](migrations/0001_initial_schema.sql) | Criação de tabelas, PKs, FKs, constraints e índices para o banco limpo |
| [`migrations/0002_dead_letter_quarantine.sql`](migrations/0002_dead_letter_quarantine.sql) | Quarentena durável das mensagens recebidas na DLQ |
| [`migrations/0003_acolhimento_operations.sql`](migrations/0003_acolhimento_operations.sql) | Registro das intenções e resultados das mutações |
| [`migrations/0004_avaliacao_titulo_nullable.sql`](migrations/0004_avaliacao_titulo_nullable.sql) | Permite vincular avaliação que ainda não possui título |

## Como Executar Localmente

### Pré-requisitos

- Rust `1.99.0` (edition 2024), fixado em `rust-toolchain.toml`, com Cargo, Clippy e rustfmt;
- PostgreSQL 15+ com banco `humanizar_acolhimento` e migrações executadas em ordem;
- RabbitMQ com permissões no vhost configurado;
- Keycloak com realm ativo e emissão de tokens.
- `AVALIACAO_PACIENTE_URL` com o endereço interno do serviço de Avaliação Paciente; o token autenticado é encaminhado à consulta, sem criar permissões adicionais no Keycloak.

### Inicialização

```bash
cargo run --locked
```

### Quality Gate (CI / Code Quality)

Para assegurar a conformidade contínua com os padrões de engenharia:

```bash
# Formatação
cargo fmt --check

# Compilação e checagem de tipos
cargo check --all-targets --locked

# Linter estrito com zero tolerância a warnings
cargo clippy --all-targets --locked -- -D warnings

# Suíte de testes unitários
cargo test --all-targets --locked
```
