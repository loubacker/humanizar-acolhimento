use lapin::BasicProperties;
use lapin::message::Delivery;
use uuid::Uuid;

use crate::application::catalog::QueueCatalog;
use crate::domain::exception::TechnicalError;
use crate::domain::model::messaging::DeadLetterMessage;

pub(crate) fn from_delivery(delivery: &Delivery) -> Result<DeadLetterMessage, TechnicalError> {
    let properties = serde_json::to_vec(&delivery.properties)
        .map_err(|error| TechnicalError::with_source("Propriedades AMQP invalidas", error))?;
    let restored: BasicProperties = serde_json::from_slice(&properties)
        .map_err(|error| TechnicalError::with_source("Propriedades AMQP invalidas", error))?;
    if restored != delivery.properties {
        return Err(TechnicalError::new("Propriedades AMQP nao preservadas"));
    }
    Ok(DeadLetterMessage {
        id: Uuid::new_v4(),
        queue_name: QueueCatalog::PATIENT_RETRIEVE_DLQ.to_owned(),
        exchange_name: delivery.exchange.to_string(),
        routing_key: delivery.routing_key.to_string(),
        payload: delivery.data.clone(),
        properties,
    })
}
