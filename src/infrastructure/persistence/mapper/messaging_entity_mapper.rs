use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::{
    ReasonCode,
    messaging::{OutboxStatus, ProcessedResult},
};
use crate::domain::model::messaging::{OutboxEvent, ProcessedEvent};
use crate::infrastructure::persistence::entity::messaging::{
    OutboxEventEntity, ProcessedEventEntity,
};

pub(crate) struct MessagingEntityMapper;

impl MessagingEntityMapper {
    pub fn to_outbox_entity(event: OutboxEvent) -> Result<OutboxEventEntity, AcolhimentoException> {
        Self::outbox(&event)
    }

    pub fn outbox(event: &OutboxEvent) -> Result<OutboxEventEntity, AcolhimentoException> {
        Ok(OutboxEventEntity {
            id: event.id(),
            event_id: event.event_id(),
            correlation_id: event.correlation_id(),
            producer_service: event.producer_service().to_owned(),
            exchange_name: event.exchange_name().to_owned(),
            routing_key: event.routing_key().to_owned(),
            aggregate_type: event.aggregate_type().to_owned(),
            aggregate_id: event.aggregate_id(),
            event_version: i16::try_from(event.event_version()).map_err(invalid_number)?,
            payload: event.payload().to_owned(),
            actor_id: event.actor_id(),
            user_agent: event.user_agent().map(str::to_owned),
            origin_ip: event.origin_ip().map(str::to_owned),
            status: match event.status() {
                OutboxStatus::New => "NEW",
                OutboxStatus::Locked => "LOCKED",
                OutboxStatus::Published => "PUBLISHED",
                OutboxStatus::Failed => "FAILED",
                OutboxStatus::Dead => "DEAD",
            }
            .to_owned(),
            attempt_count: i32::try_from(event.attempt_count()).map_err(invalid_number)?,
            max_attempts: i32::try_from(event.max_attempts()).map_err(invalid_number)?,
            next_retry_at: event.next_retry_at(),
            last_error: event.last_error().map(str::to_owned),
            created_at: event.created_at(),
            published_at: event.published_at(),
            locked_by: event.locked_by(),
        })
    }

    pub fn processed(event: &ProcessedEvent) -> ProcessedEventEntity {
        ProcessedEventEntity {
            id: event.id(),
            consumer_name: event.consumer_name().to_owned(),
            event_id: event.event_id(),
            event_hash: event.event_hash().map(str::to_owned),
            response_event_id: event.response_event_id(),
            correlation_id: event.correlation_id(),
            event_type: event.event_type().to_owned(),
            aggregate_type: event.aggregate_type().to_owned(),
            aggregate_id: event.aggregate_id(),
            actor_id: event.actor_id(),
            user_agent: event.user_agent().map(str::to_owned),
            origin_ip: event.origin_ip().map(str::to_owned),
            processed_at: event.processed_at(),
            result: match event.result() {
                ProcessedResult::Success => "SUCCESS",
                ProcessedResult::Ignored => "IGNORED",
                ProcessedResult::Failed => "FAILED",
            }
            .to_owned(),
            reason_code: event.reason_code().map(|reason| reason.code().to_owned()),
            error_message: event.error_message().map(str::to_owned),
        }
    }
}

fn invalid_number(error: std::num::TryFromIntError) -> AcolhimentoException {
    AcolhimentoException::with_source(ReasonCode::UnexpectedError, error)
}
