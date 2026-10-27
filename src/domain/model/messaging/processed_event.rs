use chrono::NaiveDateTime;
use uuid::Uuid;

use crate::domain::model::enums::ReasonCode;
use crate::domain::model::enums::messaging::ProcessedResult;

#[derive(Clone, PartialEq, Eq)]
pub struct ProcessedEvent {
    id: Option<i64>,
    consumer_name: String,
    event_id: Uuid,
    event_hash: Option<String>,
    response_event_id: Option<Uuid>,
    correlation_id: Uuid,
    event_type: String,
    aggregate_type: String,
    aggregate_id: Uuid,
    actor_id: Option<Uuid>,
    user_agent: Option<String>,
    origin_ip: Option<String>,
    processed_at: NaiveDateTime,
    result: ProcessedResult,
    reason_code: Option<ReasonCode>,
    error_message: Option<String>,
}

impl ProcessedEvent {
    #[allow(clippy::too_many_arguments)]
    pub fn new(
        consumer_name: impl Into<String>,
        event_id: Uuid,
        event_hash: Option<String>,
        response_event_id: Option<Uuid>,
        correlation_id: Uuid,
        event_type: impl Into<String>,
        aggregate_type: impl Into<String>,
        aggregate_id: Uuid,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        processed_at: NaiveDateTime,
        result: ProcessedResult,
        reason_code: Option<ReasonCode>,
        error_message: Option<String>,
    ) -> Self {
        Self {
            id: None,
            consumer_name: consumer_name.into(),
            event_id,
            event_hash,
            response_event_id,
            correlation_id,
            event_type: event_type.into(),
            aggregate_type: aggregate_type.into(),
            aggregate_id,
            actor_id,
            user_agent,
            origin_ip,
            processed_at,
            result,
            reason_code,
            error_message,
        }
    }

    pub const fn id(&self) -> Option<i64> {
        self.id
    }

    pub fn consumer_name(&self) -> &str {
        &self.consumer_name
    }

    pub const fn event_id(&self) -> Uuid {
        self.event_id
    }

    pub fn event_hash(&self) -> Option<&str> {
        self.event_hash.as_deref()
    }

    pub const fn response_event_id(&self) -> Option<Uuid> {
        self.response_event_id
    }

    pub const fn correlation_id(&self) -> Uuid {
        self.correlation_id
    }

    pub fn event_type(&self) -> &str {
        &self.event_type
    }

    pub fn aggregate_type(&self) -> &str {
        &self.aggregate_type
    }

    pub const fn aggregate_id(&self) -> Uuid {
        self.aggregate_id
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

    pub const fn processed_at(&self) -> NaiveDateTime {
        self.processed_at
    }

    pub const fn result(&self) -> ProcessedResult {
        self.result
    }

    pub const fn reason_code(&self) -> Option<ReasonCode> {
        self.reason_code
    }

    pub fn error_message(&self) -> Option<&str> {
        self.error_message.as_deref()
    }
}
