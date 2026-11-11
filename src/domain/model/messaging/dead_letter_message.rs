use uuid::Uuid;

#[derive(Clone)]
pub struct DeadLetterMessage {
    pub id: Uuid,
    pub queue_name: String,
    pub exchange_name: String,
    pub routing_key: String,
    pub payload: Vec<u8>,
    pub properties: Vec<u8>,
}
