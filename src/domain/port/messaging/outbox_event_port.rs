use async_trait::async_trait;
use chrono::NaiveDateTime;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxMessage;

#[async_trait]
pub trait OutboxEventPort: Send + Sync {
    async fn claim(
        &self,
        owner: Uuid,
        batch: u32,
        now: NaiveDateTime,
        lease_until: NaiveDateTime,
    ) -> Result<Vec<OutboxMessage>, AcolhimentoException>;

    async fn complete(
        &self,
        message: &OutboxMessage,
        owner: Uuid,
        now: NaiveDateTime,
    ) -> Result<bool, AcolhimentoException>;

    async fn fail(
        &self,
        message: &OutboxMessage,
        owner: Uuid,
        next_retry_at: NaiveDateTime,
    ) -> Result<bool, AcolhimentoException>;

    async fn purge(
        &self,
        outbox_cutoff: NaiveDateTime,
        processed_cutoff: NaiveDateTime,
    ) -> Result<u64, AcolhimentoException>;
}
