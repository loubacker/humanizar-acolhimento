<div align="center">
  <h1>Humanizar - Acolhimento (Microservice)</h1>
  <p>API interna protegida para o fluxo de acolhimento do paciente no ecossistema Humanizar.</p>

  <img alt="Java" src="https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-4.0.3-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img alt="GraalVM" src="https://img.shields.io/badge/GraalVM_Native-25-E76F00?style=for-the-badge&logo=oracle&logoColor=white" />
  <img alt="RabbitMQ" src="https://img.shields.io/badge/RabbitMQ-%23FF6600.svg?style=for-the-badge&logo=rabbitmq&logoColor=white" />
  <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" />
</div>

<br/>

Servico orientado a EDA com API HTTP interna protegida. Processa comandos de acolhimento, persiste estado local, publica comandos via outbox transacional e finaliza pendencias via callback do `humanizar-nucleo-relacionamento`.

## Arquitetura e Patterns

- Hexagonal architecture (`application`, `domain`, `infrastructure`).
- Envelope inbound obrigatorio para operacoes mutaveis (`InboundEnvelopeDTO<T>`).
- Outbox transacional para publicacao confiavel de comandos RabbitMQ.
- Controle de consistencia eventual com `pending_acolhimento` e `pending_target_status`.
- Callback inbound com idempotencia por `processed_event`.
- ACK/NACK manual do RabbitMQ com politica explicita por tipo de erro.
- Execucao otimizada com Virtual Threads e opcao de runtime em binario nativo (GraalVM Native Image).

## Interfaces internas protegidas (REST)

Base path: `/api/v1/acolhimento`

- `POST /register`
  - cria acolhimento e gera comando outbound `cmd.acolhimento.created.v1`.
  - body obrigatorio: `InboundEnvelopeDTO<InboundAcolhimentoDTO>`.
- `PUT /update/{patientId}`
  - atualiza acolhimento e gera comando outbound `cmd.acolhimento.updated.v1`.
  - body obrigatorio: `InboundEnvelopeDTO<InboundAcolhimentoDTO>`.
  - regra obrigatoria: `path.patientId == payload.patientId`.
- `DELETE /delete/{patientId}`
  - remove acolhimento e gera comando outbound `cmd.acolhimento.deleted.v1`.
  - body obrigatorio: `InboundEnvelopeDTO<AcolhimentoDeleteDTO>`.
  - regra obrigatoria: `path.patientId == payload.patientId`.
- `GET /{patientId}`
  - retorna dados do acolhimento atual do paciente.

## Contrato inbound (envelope)

Shape canonico do `InboundEnvelopeDTO<T>`:

```json
{
  "correlationId": "uuid",
  "producerService": "humanizar-service",
  "occurredAt": "2026-03-13T01:30:00",
  "actorId": "uuid",
  "userAgent": "Mozilla/5.0 ...",
  "originIp": "::1",
  "payload": {}
}
```

Nos fluxos `PUT` e `DELETE`, `payload.patientId` precisa ser igual ao `{patientId}` da URL.

## Comunicacao assincrona (RabbitMQ)

### Outbound command (produz via outbox)

**Exchange `humanizar.acolhimento.command`**
- `cmd.acolhimento.created.v1`
- `cmd.acolhimento.updated.v1`
- `cmd.acolhimento.deleted.v1`

Contrato publicado: `OutboundEnvelopeDTO<T>` (metadados EDA + payload tipado).

### Inbound callback (consome)

**Exchange `humanizar.acolhimento.event`**
- `ev.acolhimento.nucleo-relacionamento.processed.v1`
- `ev.acolhimento.nucleo-relacionamento.rejected.v1`

Fila principal:
- `callback.acolhimento.nucleo-relacionamento`

DLQ:
- `callback.acolhimento.nucleo-relacionamento.dlq`

Contrato consumido: `CallbackDTO`

## ⛓️‍💥 Resiliencia e Tolerancia a Falhas

### ACK/NACK manual

`rabbitListenerContainerFactory` roda com `AcknowledgeMode.MANUAL` (config Java).

Politica no callback inbound:
- `ack`: sucesso e evento duplicado.
- `nackRetry` (`requeue=true`): erro retentavel.
- `nackDeadLetter` (`requeue=false`): parse invalido e erro nao retentavel.

Implementacao central: `RabbitAcknowledgementConfig`.

### Outbox states

- `NEW`
- `LOCKED`
- `PUBLISHED`
- `FAILED`
- `DEAD`

Com retentativa por `OutboxRetryPolicy` e controle de ownership/fencing por `instanceId`.

## 🔐 Seguranca

- API interna protegida por OAuth2 Resource Server JWT.
- JWK configurado por `AUTH_SERVER_URL`.
- Sem exposicao de endpoint publico para uso externo.

## Estrutura do projeto

```text
src/main/java/com/humanizar/acolhimento/
|-- application/
|   |-- inbound/                    # DTOs e mappers de envelope/payload
|   |-- outbound/                   # DTOs e mappers de comando/callback
|   |-- service/                    # orquestracao create/update/delete/retrieve/callback
|   `-- usecase/                    # regras de aplicacao por contexto
|-- domain/                         # modelos, enums, ports, exceptions
`-- infrastructure/                 # adapters, controllers, rabbit, outbox, persistence
```

## Como executar localmente

### Pre-requisitos
- JDK 25
- Maven 3.9+
- PostgreSQL
- RabbitMQ

### Variaveis de ambiente (`.env`)

```env
DB_URL=jdbc:postgresql://localhost:5432/humanizar_acolhimento
DB_USERNAME=postgres
DB_PASSWORD=secret
RABBITMQ_URL=amqp://admin:admin@localhost:5672
AUTH_SERVER_URL=http://localhost:8080
```

### Execucao local (JVM)

```bash
./mvnw clean install -DskipTests
./mvnw spring-boot:run
```

Porta padrao: `9099`  
Health check: `http://localhost:9099/actuator/health`

## 🐳 Docker Native (GraalVM)

O Dockerfile do modulo usa build multi-stage com GraalVM Native Image:

1. Build stage (`ghcr.io/graalvm/native-image-community:25`) compila com:
   - `./mvnw -Pnative -DskipTests native:compile`
2. Runtime stage (`debian:bookworm-slim`) executa binario nativo:
   - `/app/app-binario`

Exemplo:

```bash
docker build -t humanizar-acolhimento:native .
docker run --rm -p 9099:9099 --env-file .env humanizar-acolhimento:native
```
