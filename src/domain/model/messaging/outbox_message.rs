use std::fmt;

use uuid::Uuid;

#[derive(Clone, PartialEq, Eq)]
pub struct OutboxMessage {
    id: i64,
    event_id: Uuid,
    correlation_id: Uuid,
    exchange_name: String,
    routing_key: String,
    payload: String,
    attempt_count: u32,
    max_attempts: u32,
}

impl fmt::Debug for OutboxMessage {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("OutboxMessage")
            .field("id", &self.id)
            .field("event_id", &self.event_id)
            .field("correlation_id", &self.correlation_id)
            .field("exchange_name", &self.exchange_name)
            .field("routing_key", &self.routing_key)
            .field("payload", &"[REDACTED]")
            .field("attempt_count", &self.attempt_count)
            .field("max_attempts", &self.max_attempts)
            .finish()
    }
}

impl OutboxMessage {
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        id: i64,
        event_id: Uuid,
        correlation_id: Uuid,
        exchange_name: impl Into<String>,
        routing_key: impl Into<String>,
        payload: impl Into<String>,
        attempt_count: u32,
        max_attempts: u32,
    ) -> Self {
        Self {
            id,
            event_id,
            correlation_id,
            exchange_name: exchange_name.into(),
            routing_key: routing_key.into(),
            payload: payload.into(),
            attempt_count,
            max_attempts,
        }
    }

    pub const fn id(&self) -> i64 {
        self.id
    }

    pub const fn event_id(&self) -> Uuid {
        self.event_id
    }

    pub const fn correlation_id(&self) -> Uuid {
        self.correlation_id
    }

    pub fn exchange_name(&self) -> &str {
        &self.exchange_name
    }

    pub fn routing_key(&self) -> &str {
        &self.routing_key
    }

    pub fn payload(&self) -> &str {
        &self.payload
    }

    pub const fn attempt_count(&self) -> u32 {
        self.attempt_count
    }

    pub const fn max_attempts(&self) -> u32 {
        self.max_attempts
    }
}
