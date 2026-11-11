use chrono::NaiveDateTime;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxMessage;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::messaging::OutboxEventEntity;

pub(crate) struct OutboxEventRepository {
    database: DatabaseConfig,
}

impl OutboxEventRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn claim(
        &self,
        owner: Uuid,
        batch: u32,
        now: NaiveDateTime,
        lease_until: NaiveDateTime,
    ) -> Result<Vec<OutboxEventEntity>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let rows = client
            .query(
                r#"
                WITH pending AS (
                    SELECT id FROM outbox_event
                    WHERE status IN ('NEW', 'FAILED', 'LOCKED')
                        AND (next_retry_at IS NULL OR next_retry_at <= $1)
                    ORDER BY created_at, id
                    LIMIT $2 FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_event AS event
                SET
                    status = 'LOCKED',
                    locked_by = $3,
                    next_retry_at = $4
                FROM pending
                WHERE event.id = pending.id
                RETURNING
                    event.id,
                    event.event_id,
                    event.correlation_id,
                    event.exchange_name,
                    event.producer_service,
                    event.routing_key,
                    event.aggregate_type,
                    event.aggregate_id,
                    event.event_version,
                    event.payload,
                    event.actor_id,
                    event.user_agent,
                    event.origin_ip,
                    event.status,
                    event.attempt_count,
                    event.max_attempts,
                    event.next_retry_at,
                    event.last_error,
                    event.created_at,
                    event.published_at,
                    event.locked_by
                "#,
                &[
                    &now.and_utc(),
                    &i64::from(batch),
                    &owner,
                    &lease_until.and_utc(),
                ],
            )
            .await
            .map_err(database_error)?;
        rows.iter()
            .map(OutboxEventEntity::from_row)
            .collect::<Result<_, _>>()
            .map_err(database_error)
    }

    pub async fn complete(
        &self,
        message: &OutboxMessage,
        owner: Uuid,
        now: NaiveDateTime,
    ) -> Result<bool, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let count = client
            .execute(
                r#"
                UPDATE outbox_event
                SET
                    status = 'PUBLISHED',
                    published_at = $4,
                    last_error = NULL,
                    locked_by = NULL
                WHERE id = $1
                    AND event_id = $2
                    AND locked_by = $3
                    AND status = 'LOCKED'
                "#,
                &[&message.id(), &message.event_id(), &owner, &now.and_utc()],
            )
            .await
            .map_err(database_error)?;
        Ok(count == 1)
    }

    pub async fn fail(
        &self,
        message: &OutboxMessage,
        owner: Uuid,
        next_retry_at: NaiveDateTime,
    ) -> Result<bool, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let count = client
            .execute(
                r#"
                UPDATE outbox_event
                SET
                    attempt_count = attempt_count + 1,
                    status = CASE WHEN attempt_count + 1 >= max_attempts THEN 'DEAD' ELSE 'FAILED' END,
                    next_retry_at = $4,
                    last_error = 'INTEGRATION_FAILURE:publish',
                    locked_by = NULL
                WHERE id = $1
                    AND event_id = $2
                    AND locked_by = $3
                    AND status = 'LOCKED'
                "#,
                &[&message.id(), &message.event_id(), &owner, &next_retry_at.and_utc()],
            )
            .await
            .map_err(database_error)?;
        Ok(count == 1)
    }

    pub async fn purge(
        &self,
        outbox_cutoff: NaiveDateTime,
        processed_cutoff: NaiveDateTime,
    ) -> Result<u64, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        let processed = transaction
            .execute(
                r#"
                WITH expired AS (
                    SELECT processed.id FROM processed_event AS processed
                    WHERE processed.processed_at < $1
                        AND NOT EXISTS (
                            SELECT 1 FROM outbox_event AS event
                            WHERE event.status IN ('NEW', 'FAILED', 'LOCKED')
                                AND (
                                    event.event_id = processed.response_event_id
                                    OR (
                                        processed.response_event_id IS NULL
                                        AND event.correlation_id = processed.correlation_id
                                    )
                                )
                        )
                    ORDER BY processed.processed_at, processed.id
                    LIMIT 1000 FOR UPDATE SKIP LOCKED
                )
                DELETE FROM processed_event AS processed
                USING expired
                WHERE processed.id = expired.id
                "#,
                &[&processed_cutoff.and_utc()],
            )
            .await
            .map_err(database_error)?;
        let outbox = transaction
            .execute(
                r#"
                WITH expired AS (
                    SELECT id FROM outbox_event
                    WHERE status = 'PUBLISHED'
                        AND created_at < $1
                    ORDER BY created_at, id
                    LIMIT 1000 FOR UPDATE SKIP LOCKED
                )
                DELETE FROM outbox_event AS event
                USING expired
                WHERE event.id = expired.id
                "#,
                &[&outbox_cutoff.and_utc()],
            )
            .await
            .map_err(database_error)?;
        transaction.commit().await.map_err(database_error)?;
        Ok(processed + outbox)
    }
}
