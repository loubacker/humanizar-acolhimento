use crate::application::catalog::ConsumerCatalog;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::infrastructure::handler::database_error;
use tokio_postgres::Transaction;
use uuid::Uuid;

pub(crate) struct ProcessedEventGuard;

impl ProcessedEventGuard {
    pub async fn lock_and_find(
        transaction: &Transaction<'_>,
        event_id: Uuid,
        event_hash: &str,
    ) -> Result<bool, AcolhimentoException> {
        let namespace = format!("{}:{event_id}", ConsumerCatalog::RETRIEVE_CONSUMER);
        transaction
            .query_one(
                "SELECT pg_advisory_xact_lock(hashtextextended($1, 0))",
                &[&namespace],
            )
            .await
            .map_err(database_error)?;
        let existing = transaction
            .query_opt(
                "SELECT id, consumer_name, event_id, event_hash, response_event_id, correlation_id, event_type, aggregate_type, aggregate_id, actor_id, user_agent, origin_ip, processed_at, result, reason_code, error_message FROM processed_events WHERE consumer_name = $1 AND event_id = $2",
                &[&ConsumerCatalog::RETRIEVE_CONSUMER, &event_id],
            )
            .await
            .map_err(database_error)?
            .as_ref().map(crate::infrastructure::persistence::entity::messaging::ProcessedEventEntity::from_row).transpose().map_err(database_error)?;
        match existing {
            None => Ok(false),
            Some(event) if event.event_hash.as_deref() == Some(event_hash) => Ok(true),
            Some(_) => Err(AcolhimentoException::new(
                ReasonCode::MessageIdentityMismatch,
            )),
        }
    }
}
