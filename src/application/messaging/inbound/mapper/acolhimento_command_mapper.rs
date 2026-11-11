use crate::application::catalog::{ExchangeCatalog, RoutingKeyCatalog};
use crate::application::messaging::inbound::dto::{
    AcolhimentoRetrieveCommandDto, InboundEnvelopeDto,
};
use crate::application::validator::required;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::AcolhimentoRetrieveCommand;
use sha2::{Digest, Sha256};

pub struct AcolhimentoCommandMapper;

impl AcolhimentoCommandMapper {
    pub fn failed_event(
        body: &[u8],
        reason: ReasonCode,
        now: chrono::NaiveDateTime,
    ) -> Option<crate::domain::model::messaging::ProcessedEvent> {
        if body.len() > 2 * 1024 * 1024 {
            return None;
        }
        let envelope: InboundEnvelopeDto<serde_json::Value> = serde_json::from_slice(body).ok()?;
        let aggregate_type = envelope
            .aggregate_type
            .filter(|value| !value.trim().is_empty() && value.chars().count() <= 255)?;
        let routing_key = envelope
            .routing_key
            .filter(|value| value.chars().count() <= 255)?;
        if envelope
            .user_agent
            .as_ref()
            .is_some_and(|value| value.chars().count() > 255)
            || envelope
                .origin_ip
                .as_ref()
                .is_some_and(|value| value.chars().count() > 255)
        {
            return None;
        }
        Some(crate::domain::model::messaging::ProcessedEvent::new(
            crate::application::catalog::ConsumerCatalog::RETRIEVE_CONSUMER,
            envelope.event_id?,
            Some(hex::encode(Sha256::digest(body))),
            None,
            envelope.correlation_id?,
            routing_key,
            aggregate_type,
            envelope.aggregate_id?,
            envelope.actor_id,
            envelope.user_agent,
            envelope.origin_ip,
            now,
            crate::domain::model::enums::messaging::ProcessedResult::Failed,
            Some(reason),
            Some(reason.message().to_owned()),
        ))
    }

    pub fn parse(
        body: &[u8],
        exchange: &str,
        routing_key: &str,
    ) -> Result<AcolhimentoRetrieveCommand, AcolhimentoException> {
        if body.len() > 2 * 1024 * 1024 {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        let envelope: InboundEnvelopeDto<AcolhimentoRetrieveCommandDto> =
            serde_json::from_slice(body).map_err(|error| {
                AcolhimentoException::with_source(ReasonCode::InboundParseError, error)
            })?;
        let missing = || AcolhimentoException::new(ReasonCode::InboundRequiredField);
        let event_id = envelope.event_id.ok_or_else(missing)?;
        let correlation_id = envelope.correlation_id.ok_or_else(missing)?;
        required(envelope.producer_service, ReasonCode::InboundRequiredField)?;
        required(envelope.aggregate_type, ReasonCode::InboundRequiredField)?;
        envelope.aggregate_id.ok_or_else(missing)?;
        let _occurred_at = envelope.occurred_at.ok_or_else(missing)?;
        if envelope.event_version != Some(1) {
            return Err(AcolhimentoException::new(
                ReasonCode::UnsupportedEventVersion,
            ));
        }
        if routing_key != RoutingKeyCatalog::PATIENT_RETRIEVE_V1
            || envelope.routing_key.as_deref() != Some(routing_key)
        {
            return Err(AcolhimentoException::new(ReasonCode::UnsupportedRoutingKey));
        }
        if exchange != ExchangeCatalog::ACOLHIMENTO_COMMAND
            || envelope.exchange_name.as_deref() != Some(exchange)
        {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        if envelope
            .user_agent
            .as_ref()
            .is_some_and(|value| value.chars().count() > 255)
            || envelope
                .origin_ip
                .as_ref()
                .is_some_and(|value| value.chars().count() > 255)
        {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        let payload = envelope.payload.ok_or_else(missing)?;
        let patient_id = payload.patient_id.ok_or_else(missing)?;
        let callback_exchange =
            required(payload.callback_exchange, ReasonCode::InboundRequiredField)?;
        if callback_exchange.len() > 255 {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        Ok(AcolhimentoRetrieveCommand {
            event_id,
            event_hash: hex::encode(Sha256::digest(body)),
            correlation_id,
            patient_id,
            callback_exchange,
            actor_id: envelope.actor_id,
            user_agent: envelope.user_agent,
            origin_ip: envelope.origin_ip,
        })
    }
}
