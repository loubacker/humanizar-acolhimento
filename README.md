<div align="center">
  <h1>Humanizar - Acolhimento (Microservice)</h1>
  <p>API interna protegida para o fluxo de acolhimento do paciente no ecossistema Humanizar.</p>

  <img alt="Java" src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img alt="GraalVM" src="https://img.shields.io/badge/GraalVM_Native-25-E76F00?style=for-the-badge&logo=oracle&logoColor=white" />
  <img alt="RabbitMQ" src="https://img.shields.io/badge/RabbitMQ-%23FF6600.svg?style=for-the-badge&logo=rabbitmq&logoColor=white" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" />
</div>

<br/>

Serviço orientado a EDA, com API HTTP interna protegida. Processa comandos de acolhimento, persiste estado local, publica comandos via outbox transacional e finaliza pendências por meio de callbacks do `humanizar-nucleo-relacionamento` e `humanizar-programa-atendimento`.

## Arquitetura e Padrões

- Arquitetura Hexagonal (`application`, `domain`, `infrastructure`).
- Envelope inbound obrigatório para operações mutáveis (`InboundEnvelopeDTO<T>`).
- Outbox transacional para publicação confiável de comandos RabbitMQ.
- Controle de consistência eventual com `pending_acolhimento` e `pending_target_status`.
- Callback inbound com idempotência por `processed_event`.
- Reconciliação ativa de pendências por worker de recall (re-emite comandos não confirmados).
- Anamnese local por núcleo (CRUD próprio), auditada via outbox sem entrega downstream.
- ACK/NACK manual do RabbitMQ com política explícita por tipo de erro.
- Execução otimizada com Virtual Threads e opção de runtime em binário nativo (GraalVM Native Image).

## Interfaces internas protegidas (REST)

Base path: `/api/v1/acolhimento`

- `POST /register`
    - Cria acolhimento e gera o comando outbound `cmd.acolhimento.created.v1`.
    - Body obrigatório: `InboundEnvelopeDTO<InboundAcolhimentoDTO>`.
- `PUT /update/{patientId}`
    - Atualiza acolhimento e gera o comando outbound `cmd.acolhimento.updated.v1`.
    - Body obrigatório: `InboundEnvelopeDTO<InboundAcolhimentoDTO>`.
    - Regra obrigatória: `path.patientId == payload.patientId`.
- `DELETE /delete/{patientId}`
    - Remove o acolhimento via remoção ordenada: `cmd.acolhimento.deleted.v2` (núcleo) e, após o callback, `cmd.acolhimento.deleted.v1` (programa).
    - Body obrigatório: `InboundEnvelopeDTO<AcolhimentoDeleteDTO>`.
    - Regra obrigatória: `path.patientId == payload.patientId`.
- `GET /{patientId}`
    - Retorna os núcleos do paciente com responsáveis e anamnese embutida (0..1 por núcleo).
    - Retry automático em falhas transientes de banco (`@Retry`, max 2, timeout 30s).

### Anamnese (CRUD local por núcleo)

Base path: `/api/v1/acolhimento/anamnese`

- `POST /{patientId}/{nucleoId}` — registra a anamnese do núcleo.
- `PUT /{patientId}/{nucleoId}` — atualiza a anamnese.
- `DELETE /{patientId}/{nucleoId}` — remove a anamnese.
- `GET /{patientId}/{nucleoId}` — retorna a anamnese do núcleo.

Mutações exigem `HttpEnvelopeDTO<AnamneseDTO>`. A anamnese é local (não trafega downstream) e apenas auditada (outbox `AUDIT`).

## 🔄 Comunicação Assíncrona (RabbitMQ)

### Outbound

**Exchange `humanizar.acolhimento.command`**
- `cmd.acolhimento.created.v1`
- `cmd.acolhimento.updated.v1`
- `cmd.acolhimento.deleted.v2` (núcleo)
- `cmd.acolhimento.deleted.v1` (programa)

Contrato publicado: `OutboundEnvelopeDTO<T>` (metadados EDA + payload tipado). DELETE é uma remoção ordenada: `deleted.v2` ao núcleo e, após o callback, `deleted.v1` ao programa.

### Inbound — Callbacks do Núcleo de Relacionamento

**Exchange `humanizar.acolhimento.event`**
- `ev.acolhimento.nucleo-relacionamento.processed.v1`
- `ev.acolhimento.nucleo-relacionamento.rejected.v1`

Fila principal:
- `callback.acolhimento.nucleo-relacionamento`

DLQ:
- `callback.acolhimento.nucleo-relacionamento.dlq`

### Inbound — Callbacks do Programa de Atendimento

**Exchange `humanizar.acolhimento.event`**
- `ev.acolhimento.programa.processed.v1`
- `ev.acolhimento.programa.rejected.v1`

Fila principal:
- `callback.acolhimento.programa`

DLQ:
- `callback.acolhimento.programa.dlq`

Contrato consumido (ambos): `CallbackDTO`.

## ⛓️‍💥 Resiliência e Tolerância a Falhas

### ACK/NACK manual

`rabbitListenerContainerFactory` roda com `AcknowledgeMode.MANUAL` (configuração Java).

Política no callback inbound:
- `ack`: sucesso e evento duplicado.
- `nackRetry` (`requeue=true`): erro retentável.
- `nackDeadLetter` (`requeue=false`): parse inválido e erro não retentável.

Implementação central: `RabbitAcknowledgementConfig`.

### Retry transiente (endpoint GET)

`@Retry` via `ResilientMethodsConfig` (Spring Framework 7 `@Retryable`).

- Max retries: 2, timeout: 30s.
- Predicate: `TransientDataAccessException`, `RecoverableDataAccessException`, `CannotCreateTransactionException`, `QueryTimeoutException`.

### Outbox states

- `NEW`
- `LOCKED`
- `PUBLISHED`
- `FAILED`
- `DEAD`
- `AUDIT` (auditoria local da anamnese; nunca publicado)

Com retentativa por `OutboxRetryPolicy` e controle de ownership/fencing por `instanceId`.

## 🛠️ Workers em Background

### OutboxRelayWorker (5s)

- Faz claim de eventos em `NEW`, `FAILED` e `LOCKED`.
- Aplica lock ownership por `instanceId` (fencing).
- Publica de forma assíncrona com controle de paralelismo.

### OutboxEventProcessor

Transições de status:
- sucesso: `LOCKED -> PUBLISHED`
- falha com retry: `LOCKED -> FAILED` (com `nextRetryAt`)
- tentativas esgotadas: `LOCKED -> DEAD`

### PendingRecallWorker (30s)

- Re-dirige pendências presas (`pending_target_status` em `PENDING` e vencidas) resetando o outbox para `NEW`.
- Backoff por `RecallPolicy`; escala para `ERROR` após o limite de tentativas.

### RetentionWorker (1h)

- Remove outbox antigo (`PUBLISHED` e `DEAD`) com mais de 48h.
- Remove `processed_event` antigo com mais de 90 dias.
- Remove pendências de saga concluídas (`pending_acolhimento`/`pending_target_status` terminais) com mais de 90 dias.

## 🔐 Segurança

- API interna protegida por OAuth2 Resource Server JWT.
- JWK configurado por `AUTH_SERVER_URL`.
- Sem exposição de endpoint público para uso externo.

## Estrutura do projeto

```text
src/main/java/com/humanizar/acolhimento/
|-- application/
|   |-- catalog/                    # Exchange/Queue/RoutingKey/Target/ProducerCatalog
|   |-- http/                       # DTOs, mappers e validacao de envelope/payload
|   |-- messaging/                  # inbound (callbacks) e outbound (comandos/audit)
|   |-- service/                    # orquestracao create/update/delete/retrieve/callback/anamnese
|   `-- usecase/                    # regras de aplicacao por contexto
|-- domain/                         # modelos, enums, ports, exceptions
`-- infrastructure/                 # adapters, controllers, rabbit, outbox, persistence
```

### Pré-requisitos
- JDK 25
- Maven 3.9+
- PostgreSQL
- RabbitMQ

### Variáveis de Ambiente (`.env`)

```env
DB_URL=jdbc:postgresql://localhost:5432/db
DB_USERNAME=postgres
DB_PASSWORD=secret
RABBITMQ_URL=amqp://admin:admin@localhost:5672
AUTH_SERVER_URL=http://localhost:8080
```

### Execução local (JVM)

```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

Porta padrao: `9099`  
Health check: `http://localhost:9099/actuator/health`

## 🐳 Docker Native (GraalVM)

O Dockerfile do módulo usa build multi-stage com GraalVM Native Image:

1. Build stage (`ghcr.io/graalvm/native-image-community:25`) compila com:
   - `./mvnw -Pnative -DskipTests native:compile`
2. Runtime stage (`debian:bookworm-slim`) executa binario nativo:
   - `/app/app-binario`

Exemplo:

```bash
docker build -t humanizar-acolhimento:native .
docker run --rm -p 9099:9099 --env-file .env humanizar-acolhimento:native
```
