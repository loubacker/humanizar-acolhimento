use crate::application::catalog::QueueCatalog;
use crate::domain::exception::TechnicalError;
use crate::domain::port::messaging::DeadLetterQuarantinePort;
use crate::infrastructure::config::rabbit::{RabbitAcknowledgementConfig, rabbit_error};
use futures_util::StreamExt;
use lapin::options::{BasicConsumeOptions, BasicQosOptions};
use lapin::types::FieldTable;
use lapin::{Channel, Consumer};
use std::sync::Arc;
use std::time::Duration;
use tokio_util::sync::CancellationToken;

use super::dead_letter_delivery_mapper;

pub(crate) async fn subscribe(channel: &Channel) -> Result<Consumer, TechnicalError> {
    channel
        .basic_qos(20, BasicQosOptions::default())
        .await
        .map_err(rabbit_error)?;
    channel
        .basic_consume(
            QueueCatalog::PATIENT_RETRIEVE_DLQ.into(),
            "acolhimento.dlq".into(),
            BasicConsumeOptions::default(),
            FieldTable::default(),
        )
        .await
        .map_err(rabbit_error)
}

pub(crate) async fn run(
    mut consumer: Consumer,
    quarantine: Arc<dyn DeadLetterQuarantinePort>,
    retry_delay: Duration,
    stop: CancellationToken,
) -> Result<(), TechnicalError> {
    loop {
        let delivery = tokio::select! {
            _ = stop.cancelled() => return Ok(()),
            delivery = consumer.next() => delivery.ok_or_else(|| TechnicalError::new("Consumer DLQ encerrado"))?.map_err(rabbit_error)?,
        };
        let message = loop {
            match dead_letter_delivery_mapper::from_delivery(&delivery) {
                Ok(message) => break message,
                Err(_) => {
                    tracing::error!(
                        service = "humanizar-acolhimento",
                        phase = "dead_letter_mapping",
                        outcome = "retry"
                    );
                    tokio::select! {
                        _ = stop.cancelled() => return Ok(()),
                        _ = tokio::time::sleep(retry_delay) => {}
                    }
                }
            }
        };
        loop {
            match quarantine.store(&message).await {
                Ok(()) => break,
                Err(_) => {
                    tracing::error!(service = "humanizar-acolhimento", phase = "dead_letter_quarantine", outcome = "retry", quarantine_id = %message.id);
                    tokio::select! {
                        _ = stop.cancelled() => return Ok(()),
                        _ = tokio::time::sleep(retry_delay) => {}
                    }
                }
            }
        }
        RabbitAcknowledgementConfig::ack(&delivery).await?;
        tracing::error!(
            service = "humanizar-acolhimento",
            phase = "dead_letter",
            outcome = "quarantined",
            queue = QueueCatalog::PATIENT_RETRIEVE_DLQ,
            quarantine_id = %message.id,
        );
    }
}
