use crate::application::catalog::{ConsumerCatalog, RoutingKeyCatalog};
use crate::application::messaging::outbound::factory::AcolhimentoCallbackFactory;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::{AcolhimentoRetrieveCommand, ProcessingOutcome};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;

pub(crate) struct ProcessedEventRepository {
    database: DatabaseConfig,
    max_attempts: u32,
}

impl ProcessedEventRepository {
    pub fn new(database: DatabaseConfig, max_attempts: u32) -> Self {
        Self {
            database,
            max_attempts,
        }
    }

    pub async fn process(
        &self,
        command: AcolhimentoRetrieveCommand,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        if crate::infrastructure::messaging::inbound::idempotency::ProcessedEventGuard::lock_and_find(
            &transaction,
            command.event_id,
            &command.event_hash,
        )
        .await?
        {
            transaction.commit().await.map_err(database_error)?;
            return Ok(ProcessingOutcome::Duplicate);
        }

        let has_acolhimento: bool = transaction
            .query_one(
                "SELECT EXISTS(SELECT 1 FROM nucleo_patient WHERE patient_id = $1 AND desvinculado_at IS NULL)",
                &[&command.patient_id],
            )
            .await
            .map_err(database_error)?
            .get(0);

        let now = chrono::Utc::now().naive_utc();
        let event =
            AcolhimentoCallbackFactory::create(&command, has_acolhimento, now, self.max_attempts)?;
        let entity =
            crate::infrastructure::persistence::mapper::MessagingEntityMapper::outbox(&event)?;
        if entity.id.is_some() {
            return Err(AcolhimentoException::new(ReasonCode::UnexpectedError));
        }

        transaction
            .execute(
                r#"
                INSERT INTO outbox_event (
                    event_id,
                    correlation_id,
                    producer_service,
                    exchange_name,
                    routing_key,
                    aggregate_type,
                    aggregate_id,
                    event_version,
                    payload,
                    actor_id,
                    user_agent,
                    origin_ip,
                    status,
                    attempt_count,
                    max_attempts,
                    next_retry_at,
                    last_error,
                    created_at,
                    published_at,
                    locked_by
                )
                VALUES (
                    $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
                    $11, $12, $13, $14, $15, $16, $17, $18, $19, $20
                )
                "#,
                &[
                    &entity.event_id,
                    &entity.correlation_id,
                    &entity.producer_service,
                    &entity.exchange_name,
                    &entity.routing_key,
                    &entity.aggregate_type,
                    &entity.aggregate_id,
                    &entity.event_version,
                    &entity.payload,
                    &entity.actor_id,
                    &entity.user_agent,
                    &entity.origin_ip,
                    &entity.status,
                    &entity.attempt_count,
                    &entity.max_attempts,
                    &entity.next_retry_at,
                    &entity.last_error,
                    &entity.created_at,
                    &entity.published_at,
                    &entity.locked_by,
                ],
            )
            .await
            .map_err(database_error)?;

        let receipt = crate::domain::model::messaging::ProcessedEvent::new(
            ConsumerCatalog::RETRIEVE_CONSUMER,
            command.event_id,
            Some(command.event_hash),
            Some(event.event_id()),
            command.correlation_id,
            RoutingKeyCatalog::PATIENT_RETRIEVE_V1,
            event.aggregate_type(),
            command.patient_id,
            command.actor_id,
            command.user_agent,
            command.origin_ip,
            now,
            crate::domain::model::enums::messaging::ProcessedResult::Success,
            None,
            None,
        );
        save_receipt(&transaction, &receipt).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(ProcessingOutcome::Processed)
    }

    pub async fn record_failure(
        &self,
        event: crate::domain::model::messaging::ProcessedEvent,
    ) -> Result<ProcessingOutcome, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        if crate::infrastructure::messaging::inbound::idempotency::ProcessedEventGuard::lock_and_find(
            &transaction,
            event.event_id(),
            event.event_hash().ok_or_else(|| AcolhimentoException::new(ReasonCode::MessageIdentityMismatch))?,
        )
        .await?
        {
            transaction.commit().await.map_err(database_error)?;
            return Ok(ProcessingOutcome::Duplicate);
        }
        save_receipt(&transaction, &event).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(ProcessingOutcome::Processed)
    }
}

async fn save_receipt(
    transaction: &tokio_postgres::Transaction<'_>,
    receipt: &crate::domain::model::messaging::ProcessedEvent,
) -> Result<(), AcolhimentoException> {
    let entity =
        crate::infrastructure::persistence::mapper::MessagingEntityMapper::processed(receipt);
    if entity.id.is_some() {
        return Err(AcolhimentoException::new(ReasonCode::UnexpectedError));
    }
    transaction
        .execute(
            r#"
            INSERT INTO processed_event (
                consumer_name,
                event_id,
                event_hash,
                response_event_id,
                correlation_id,
                event_type,
                aggregate_type,
                aggregate_id,
                actor_id,
                user_agent,
                origin_ip,
                processed_at,
                result,
                reason_code,
                error_message
            )
            VALUES (
                $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
                $11, $12, $13, $14, $15
            )
            "#,
            &[
                &entity.consumer_name,
                &entity.event_id,
                &entity.event_hash,
                &entity.response_event_id,
                &entity.correlation_id,
                &entity.event_type,
                &entity.aggregate_type,
                &entity.aggregate_id,
                &entity.actor_id,
                &entity.user_agent,
                &entity.origin_ip,
                &entity.processed_at,
                &entity.result,
                &entity.reason_code,
                &entity.error_message,
            ],
        )
        .await
        .map_err(database_error)?;

    Ok(())
}
