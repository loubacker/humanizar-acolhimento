use chrono::NaiveDateTime;
use uuid::Uuid;

pub(crate) struct OutboxEventEntity {
    pub id: Option<i64>,
    pub event_id: Uuid,
    pub correlation_id: Uuid,
    pub producer_service: String,
    pub exchange_name: String,
    pub routing_key: String,
    pub aggregate_type: String,
    pub aggregate_id: Uuid,
    pub event_version: i16,
    pub payload: String,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
    pub status: String,
    pub attempt_count: i32,
    pub max_attempts: i32,
    pub next_retry_at: Option<NaiveDateTime>,
    pub last_error: Option<String>,
    pub created_at: NaiveDateTime,
    pub published_at: Option<NaiveDateTime>,
    pub locked_by: Option<Uuid>,
}

impl OutboxEventEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            event_id: row.try_get("event_id")?,
            correlation_id: row.try_get("correlation_id")?,
            producer_service: row.try_get("producer_service")?,
            exchange_name: row.try_get("exchange_name")?,
            routing_key: row.try_get("routing_key")?,
            aggregate_type: row.try_get("aggregate_type")?,
            aggregate_id: row.try_get("aggregate_id")?,
            event_version: row.try_get("event_version")?,
            payload: row.try_get("payload")?,
            actor_id: row.try_get("actor_id")?,
            user_agent: row.try_get("user_agent")?,
            origin_ip: row.try_get("origin_ip")?,
            status: row.try_get("status")?,
            attempt_count: row.try_get("attempt_count")?,
            max_attempts: row.try_get("max_attempts")?,
            next_retry_at: row.try_get("next_retry_at")?,
            last_error: row.try_get("last_error")?,
            created_at: row.try_get("created_at")?,
            published_at: row.try_get("published_at")?,
            locked_by: row.try_get("locked_by")?,
        })
    }
}
