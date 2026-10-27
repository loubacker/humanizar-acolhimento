use chrono::NaiveDateTime;
use serde::Serialize;
use uuid::Uuid;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct OutboundEnvelopeDto<T> {
    pub event_id: Uuid,
    pub correlation_id: Uuid,
    pub producer_service: String,
    pub exchange_name: String,
    pub routing_key: String,
    pub aggregate_type: String,
    pub aggregate_id: Uuid,
    pub event_version: u16,
    pub occurred_at: NaiveDateTime,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
    pub payload: T,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoRetrievedDto {
    pub request_event_id: Uuid,
    pub patient_id: Uuid,
    pub status: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AcolhimentoRejectedDto {
    pub request_event_id: Uuid,
    pub patient_id: Uuid,
    pub reason_code: String,
    pub message: String,
}
