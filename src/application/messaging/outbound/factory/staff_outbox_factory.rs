use chrono::NaiveDateTime;
use serde::Serialize;
use uuid::Uuid;

use crate::application::catalog::{
    AggregateCatalog, ExchangeCatalog, ProducerCatalog, RoutingKeyCatalog,
};
use crate::application::messaging::outbound::dto::{
    OutboundEnvelopeDto, StaffDesvinculadoDto, StaffVinculadoDto,
};
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::OutboxEvent;

pub struct StaffOutboxFactory;

impl StaffOutboxFactory {
    pub fn create_vinculado(
        correlation_id: Uuid,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        payload: StaffVinculadoDto,
        now: NaiveDateTime,
        max_attempts: u32,
    ) -> Result<OutboxEvent, AcolhimentoException> {
        let aggregate_id = payload.nucleo_patient_id;
        Self::encode(
            correlation_id,
            RoutingKeyCatalog::STAFF_VINCULADO_V1,
            aggregate_id,
            actor_id,
            user_agent,
            origin_ip,
            payload,
            now,
            max_attempts,
        )
    }

    pub fn create_desvinculado(
        correlation_id: Uuid,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        payload: StaffDesvinculadoDto,
        now: NaiveDateTime,
        max_attempts: u32,
    ) -> Result<OutboxEvent, AcolhimentoException> {
        let aggregate_id = payload.nucleo_patient_id;
        Self::encode(
            correlation_id,
            RoutingKeyCatalog::STAFF_DESVINCULADO_V1,
            aggregate_id,
            actor_id,
            user_agent,
            origin_ip,
            payload,
            now,
            max_attempts,
        )
    }

    fn encode<T: Serialize>(
        correlation_id: Uuid,
        routing_key: &str,
        aggregate_id: Uuid,
        actor_id: Option<Uuid>,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        payload: T,
        now: NaiveDateTime,
        max_attempts: u32,
    ) -> Result<OutboxEvent, AcolhimentoException> {
        let event_id = Uuid::new_v4();
        let envelope = OutboundEnvelopeDto {
            event_id,
            correlation_id,
            producer_service: ProducerCatalog::ACOLHIMENTO_SERVICE.to_owned(),
            exchange_name: ExchangeCatalog::ACOLHIMENTO_EVENT.to_owned(),
            routing_key: routing_key.to_owned(),
            aggregate_type: AggregateCatalog::NUCLEO_PATIENT.to_owned(),
            aggregate_id,
            event_version: 1,
            occurred_at: now,
            actor_id,
            user_agent: user_agent.clone(),
            origin_ip: origin_ip.clone(),
            payload,
        };
        let serialized = serde_json::to_string(&envelope).map_err(|error| {
            AcolhimentoException::with_source(ReasonCode::UnexpectedError, error)
        })?;
        Ok(OutboxEvent::create(
            event_id,
            correlation_id,
            ProducerCatalog::ACOLHIMENTO_SERVICE,
            ExchangeCatalog::ACOLHIMENTO_EVENT.to_owned(),
            routing_key,
            AggregateCatalog::NUCLEO_PATIENT,
            aggregate_id,
            1,
            serialized,
            actor_id,
            user_agent,
            origin_ip,
            max_attempts,
            now,
        ))
    }
}
