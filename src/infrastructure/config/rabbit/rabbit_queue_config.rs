use super::rabbit_error;
use crate::application::catalog::QueueCatalog;
use crate::domain::exception::TechnicalError;
use lapin::Channel;
use lapin::options::QueueDeclareOptions;
use lapin::types::{AMQPValue, FieldTable, LongString};

pub(crate) struct RabbitQueueConfig;

impl RabbitQueueConfig {
    pub async fn declare(channel: &Channel, delivery_limit: i32) -> Result<(), TechnicalError> {
        let mut arguments = FieldTable::default();
        arguments.insert(
            "x-queue-type".into(),
            AMQPValue::LongString(LongString::from("quorum")),
        );
        channel
            .queue_declare(
                QueueCatalog::PATIENT_RETRIEVE_DLQ.into(),
                QueueDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                arguments.clone(),
            )
            .await
            .map_err(rabbit_error)?;
        arguments.insert(
            "x-delivery-limit".into(),
            AMQPValue::LongInt(delivery_limit),
        );
        arguments.insert(
            "x-dead-letter-exchange".into(),
            AMQPValue::LongString(LongString::from("")),
        );
        arguments.insert(
            "x-dead-letter-routing-key".into(),
            AMQPValue::LongString(LongString::from(QueueCatalog::PATIENT_RETRIEVE_DLQ)),
        );
        channel
            .queue_declare(
                QueueCatalog::PATIENT_RETRIEVE.into(),
                QueueDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                arguments,
            )
            .await
            .map_err(rabbit_error)?;
        Ok(())
    }
}
