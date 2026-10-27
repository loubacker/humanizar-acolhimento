use crate::application::catalog::QueueCatalog;
use crate::domain::exception::TechnicalError;
use crate::infrastructure::config::rabbit::rabbit_error;
use futures_util::StreamExt;
use lapin::options::{BasicAckOptions, BasicConsumeOptions, BasicQosOptions};
use lapin::types::FieldTable;
use lapin::{Channel, Consumer};
use tokio_util::sync::CancellationToken;

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
    stop: CancellationToken,
) -> Result<(), TechnicalError> {
    loop {
        let delivery = tokio::select! {
            _ = stop.cancelled() => return Ok(()),
            delivery = consumer.next() => delivery.ok_or_else(|| TechnicalError::new("Consumer DLQ encerrado"))?.map_err(rabbit_error)?,
        };
        tracing::error!(
            service = "humanizar-acolhimento",
            phase = "dead_letter",
            outcome = "discarded",
            queue = QueueCatalog::PATIENT_RETRIEVE_DLQ
        );
        if !delivery
            .ack(BasicAckOptions::default())
            .await
            .map_err(rabbit_error)?
        {
            return Err(TechnicalError::new("ACK de DLQ nao enviado"));
        }
    }
}
