use chrono::NaiveDateTime;
use uuid::Uuid;

pub(crate) struct ProcessedEventEntity {
    pub id: Option<i64>,
    pub consumer_name: String,
    pub event_id: Uuid,
    pub event_hash: Option<String>,
    pub response_event_id: Option<Uuid>,
    pub correlation_id: Uuid,
    pub event_type: String,
    pub aggregate_type: String,
    pub aggregate_id: Uuid,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
    pub processed_at: NaiveDateTime,
    pub result: String,
    pub reason_code: Option<String>,
    pub error_message: Option<String>,
}

impl ProcessedEventEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            consumer_name: row.try_get("consumer_name")?,
            event_id: row.try_get("event_id")?,
            event_hash: row.try_get("event_hash")?,
            response_event_id: row.try_get("response_event_id")?,
            correlation_id: row.try_get("correlation_id")?,
            event_type: row.try_get("event_type")?,
            aggregate_type: row.try_get("aggregate_type")?,
            aggregate_id: row.try_get("aggregate_id")?,
            actor_id: row.try_get("actor_id")?,
            user_agent: row.try_get("user_agent")?,
            origin_ip: row.try_get("origin_ip")?,
            processed_at: row.try_get("processed_at")?,
            result: row.try_get("result")?,
            reason_code: row.try_get("reason_code")?,
            error_message: row.try_get("error_message")?,
        })
    }
}
