use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::DeadLetterMessage;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;

pub(crate) struct DeadLetterQuarantineRepository {
    database: DatabaseConfig,
}

impl DeadLetterQuarantineRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn store(&self, message: &DeadLetterMessage) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        let stored = client
            .query_opt(
                r#"
                INSERT INTO dead_letter_quarantine AS quarantine (
                    id, queue_name, exchange_name, routing_key, payload, properties
                ) VALUES ($1, $2, $3, $4, $5, $6)
                ON CONFLICT (id) DO UPDATE SET id = quarantine.id
                WHERE quarantine.queue_name = EXCLUDED.queue_name
                    AND quarantine.exchange_name = EXCLUDED.exchange_name
                    AND quarantine.routing_key = EXCLUDED.routing_key
                    AND quarantine.payload = EXCLUDED.payload
                    AND quarantine.properties = EXCLUDED.properties
                RETURNING id
                "#,
                &[
                    &message.id,
                    &message.queue_name,
                    &message.exchange_name,
                    &message.routing_key,
                    &message.payload,
                    &message.properties,
                ],
            )
            .await
            .map_err(database_error)?;
        if stored.is_none() {
            return Err(AcolhimentoException::new(ReasonCode::DuplicateEvent));
        }
        Ok(())
    }
}
