use std::fmt;

use chrono::NaiveDateTime;
use uuid::Uuid;

use crate::domain::model::enums::messaging::OutboxStatus;

#[derive(Clone, PartialEq, Eq)]
pub struct OutboxEvent {
    id: Option<i64>,
    event_id: Uuid,
    correlation_id: Uuid,
    producer_service: String,
    exchange_name: String,
    routing_key: String,
    aggregate_type: String,
    aggregate_id: Uuid,
    event_version: u16,
    payload: String,
    actor_id: Option<Uuid>,
    user_agent: Option<String>,
    origin_ip: Option<String>,
    status: OutboxStatus,
    attempt_count: u32,
    max_attempts: u32,
    next_retry_at: Option<NaiveDateTime>,
    last_error: Option<String>,
    created_at: NaiveDateTime,
    published_at: Option<NaiveDateTime>,
    locked_by: Option<Uuid>,
}

impl fmt::Debug for OutboxEvent {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        formatter
            .debug_struct("OutboxEvent")
            .field("id", &self.id)
            .field("event_id", &self.event_id)
            .field("correlation_id", &self.correlation_id)
            .field("producer_service", &self.producer_service)
            .field("exchange_name", &self.exchange_name)
            .field("routing_key", &self.routing_key)
            .field("aggregate_type", &self.aggregate_type)
            .field("aggregate_id", &self.aggregate_id)
            .field("event_version", &self.event_version)
            .field("payload", &"[REDACTED]")
            .field("actor_id", &self.actor_id)
            .field(
                "user_agent",
                &self.user_agent.as_ref().map(|_| "[REDACTED]"),
            )
            .field("origin_ip", &self.origin_ip.as_ref().map(|_| "[REDACTED]"))
            .field("status", &self.status)
            .field("attempt_count", &self.attempt_count)
            .field("max_attempts", &self.max_attempts)
            .field("next_retry_at", &self.next_retry_at)
            .field(
                "last_error",
                &self.last_error.as_ref().map(|_| "[REDACTED]"),
            )
            .field("created_at", &self.created_at)
            .field("published_at", &self.published_at)
            .field("locked_by", &self.locked_by)
            .finish()
    }
}

impl OutboxEvent {
    #[allow(clippy::too_many_arguments)]
    pub fn create(
        event_id: Uuid,
        correlation_id: Uuid,
        producer_service: impl Into<String>,
        exchange_name: impl Into<String>,
        routing_key: impl Into<String>,
        aggregate_type: impl Into<String>,
        aggregate_id: Uuid,
        event_version: u16,
        payload: impl Into<String>,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        max_attempts: u32,
        created_at: NaiveDateTime,
    ) -> Self {
        Self {
            id: None,
            event_id,
            correlation_id,
            producer_service: producer_service.into(),
            exchange_name: exchange_name.into(),
            routing_key: routing_key.into(),
            aggregate_type: aggregate_type.into(),
            aggregate_id,
            event_version,
            payload: payload.into(),
            actor_id,
            user_agent,
            origin_ip,
            status: OutboxStatus::New,
            attempt_count: 0,
            max_attempts,
            next_retry_at: Some(created_at),
            last_error: None,
            created_at,
            published_at: None,
            locked_by: None,
        }
    }

    #[allow(clippy::too_many_arguments)]
    pub fn restore(
        id: Option<i64>,
        event_id: Uuid,
        correlation_id: Uuid,
        producer_service: String,
        exchange_name: String,
        routing_key: String,
        aggregate_type: String,
        aggregate_id: Uuid,
        event_version: u16,
        payload: String,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        status: OutboxStatus,
        attempt_count: u32,
        max_attempts: u32,
        next_retry_at: Option<NaiveDateTime>,
        last_error: Option<String>,
        created_at: NaiveDateTime,
        published_at: Option<NaiveDateTime>,
        locked_by: Option<Uuid>,
    ) -> Self {
        Self {
            id,
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
            locked_by,
        }
    }

    pub const fn id(&self) -> Option<i64> {
        self.id
    }

    pub const fn event_id(&self) -> Uuid {
        self.event_id
    }

    pub const fn correlation_id(&self) -> Uuid {
        self.correlation_id
    }

    pub fn producer_service(&self) -> &str {
        &self.producer_service
    }

    pub fn exchange_name(&self) -> &str {
        &self.exchange_name
    }

    pub fn routing_key(&self) -> &str {
        &self.routing_key
    }

    pub fn aggregate_type(&self) -> &str {
        &self.aggregate_type
    }

    pub const fn aggregate_id(&self) -> Uuid {
        self.aggregate_id
    }

    pub const fn event_version(&self) -> u16 {
        self.event_version
    }

    pub fn payload(&self) -> &str {
        &self.payload
    }

    pub const fn actor_id(&self) -> Option<Uuid> {
        self.actor_id
    }

    pub fn user_agent(&self) -> Option<&str> {
        self.user_agent.as_deref()
    }

    pub fn origin_ip(&self) -> Option<&str> {
        self.origin_ip.as_deref()
    }

    pub const fn status(&self) -> OutboxStatus {
        self.status
    }

    pub const fn attempt_count(&self) -> u32 {
        self.attempt_count
    }

    pub const fn max_attempts(&self) -> u32 {
        self.max_attempts
    }

    pub const fn next_retry_at(&self) -> Option<NaiveDateTime> {
        self.next_retry_at
    }

    pub fn last_error(&self) -> Option<&str> {
        self.last_error.as_deref()
    }

    pub const fn created_at(&self) -> NaiveDateTime {
        self.created_at
    }

    pub const fn published_at(&self) -> Option<NaiveDateTime> {
        self.published_at
    }

    pub const fn locked_by(&self) -> Option<Uuid> {
        self.locked_by
    }
}
