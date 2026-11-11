use uuid::Uuid;

#[derive(Debug, Clone)]
pub struct AcolhimentoRetrieveCommand {
    pub event_id: Uuid,
    pub event_hash: String,
    pub correlation_id: Uuid,
    pub patient_id: Uuid,
    pub callback_exchange: String,
    pub actor_id: Option<Uuid>,
    pub user_agent: Option<String>,
    pub origin_ip: Option<String>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ProcessingOutcome {
    Processed,
    Duplicate,
}
