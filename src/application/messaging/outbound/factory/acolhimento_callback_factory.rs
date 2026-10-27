use chrono::NaiveDateTime;
use serde::Serialize;
use uuid::Uuid;

use crate::application::catalog::{AggregateCatalog, ProducerCatalog, RoutingKeyCatalog};
use crate::application::messaging::outbound::dto::OutboundEnvelopeDto;
use crate::application::messaging::outbound::mapper::AcolhimentoCallbackMapper;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::{AcolhimentoRetrieveCommand, OutboxEvent};

pub struct AcolhimentoCallbackFactory;

impl AcolhimentoCallbackFactory {
    pub fn create(
        command: &AcolhimentoRetrieveCommand,
        has_acolhimento: bool,
        now: NaiveDateTime,
        max_attempts: u32,
    ) -> Result<OutboxEvent, AcolhimentoException> {
        if has_acolhimento {
            Self::encode(
                command,
                RoutingKeyCatalog::PATIENT_RETRIEVED_V1,
                AcolhimentoCallbackMapper::retrieved(command, "ACTIVE".to_owned()),
                now,
                max_attempts,
            )
        } else {
            Self::encode(
                command,
                RoutingKeyCatalog::PATIENT_FAILED_V1,
                AcolhimentoCallbackMapper::rejected(command),
                now,
                max_attempts,
            )
        }
    }

    fn encode<T: Serialize>(
        command: &AcolhimentoRetrieveCommand,
        routing_key: &str,
        payload: T,
        now: NaiveDateTime,
        max_attempts: u32,
    ) -> Result<OutboxEvent, AcolhimentoException> {
        let event_id = Uuid::new_v4();
        let envelope = OutboundEnvelopeDto {
            event_id,
            correlation_id: command.correlation_id,
            producer_service: ProducerCatalog::ACOLHIMENTO_SERVICE.to_owned(),
            exchange_name: command.callback_exchange.clone(),
            routing_key: routing_key.to_owned(),
            aggregate_type: AggregateCatalog::ACOLHIMENTO.to_owned(),
            aggregate_id: command.patient_id,
            event_version: 1,
            occurred_at: now,
            actor_id: command.actor_id,
            user_agent: command.user_agent.clone(),
            origin_ip: command.origin_ip.clone(),
            payload,
        };
        let serialized = serde_json::to_string(&envelope).map_err(|error| {
            AcolhimentoException::with_source(ReasonCode::UnexpectedError, error)
        })?;
        Ok(OutboxEvent::create(
            event_id,
            command.correlation_id,
            ProducerCatalog::ACOLHIMENTO_SERVICE,
            command.callback_exchange.clone(),
            routing_key,
            AggregateCatalog::ACOLHIMENTO,
            command.patient_id,
            1,
            serialized,
            command.actor_id,
            command.user_agent.clone(),
            command.origin_ip.clone(),
            max_attempts,
            now,
        ))
    }
}
