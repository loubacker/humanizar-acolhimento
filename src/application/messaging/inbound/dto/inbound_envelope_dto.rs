use chrono::NaiveDateTime;
use serde::Deserialize;
use uuid::Uuid;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InboundEnvelopeDto<T> {
    pub event_id: Option<Uuid>,
    pub correlation_id: Option<Uuid>,
    pub producer_service: Option<String>,
    pub exchange_name: Option<String>,
    pub routing_key: Option<String>,
    pub aggregate_type: Option<String>,
    pub aggregate_id: Option<Uuid>,
    pub event_version: Option<u16>,
    pub occurred_at: Option<NaiveDateTime>,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
    pub payload: Option<T>,
}

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoRetrieveCommandDto {
    pub patient_id: Option<Uuid>,
    pub callback_exchange: Option<String>,
}
