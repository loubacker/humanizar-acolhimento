use super::rabbit_error;
use crate::domain::exception::TechnicalError;
use lapin::message::Delivery;
use lapin::options::{BasicAckOptions, BasicNackOptions};

pub(crate) struct RabbitAcknowledgementConfig;

impl RabbitAcknowledgementConfig {
    pub async fn ack(delivery: &Delivery) -> Result<(), TechnicalError> {
        Self::sent(
            delivery
                .ack(BasicAckOptions::default())
                .await
                .map_err(rabbit_error)?,
        )
    }

    pub async fn nack(delivery: &Delivery, retry: bool) -> Result<(), TechnicalError> {
        Self::sent(
            delivery
                .nack(BasicNackOptions {
                    multiple: false,
                    requeue: retry,
                })
                .await
                .map_err(rabbit_error)?,
        )
    }

    fn sent(sent: bool) -> Result<(), TechnicalError> {
        if !sent {
            return Err(TechnicalError::new("ACK/NACK nao enviado"));
        }
        Ok(())
    }
}
