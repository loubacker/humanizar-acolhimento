use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::OutboxMessage;
use crate::domain::port::messaging::MessagePublisherPort;
use async_trait::async_trait;
use lapin::options::{BasicPublishOptions, ConfirmSelectOptions};
use lapin::{BasicProperties, Channel};
use std::time::Duration;

pub struct RabbitOutboxPublisher {
    channel: Channel,
    timeout: Duration,
}

impl RabbitOutboxPublisher {
    pub async fn new(channel: Channel, timeout: Duration) -> Result<Self, AcolhimentoException> {
        tokio::time::timeout(
            timeout,
            channel.confirm_select(ConfirmSelectOptions::default()),
        )
        .await
        .map_err(publish_error)?
        .map_err(publish_error)?;
        Ok(Self { channel, timeout })
    }
}

#[async_trait]
impl MessagePublisherPort for RabbitOutboxPublisher {
    fn is_available(&self) -> bool {
        self.channel.status().connected()
    }

    async fn publish(&self, message: &OutboxMessage) -> Result<(), AcolhimentoException> {
        let exchange =
            lapin::types::ShortString::try_new(message.exchange_name()).map_err(publish_error)?;
        let routing_key =
            lapin::types::ShortString::try_new(message.routing_key()).map_err(publish_error)?;
        let publication = async {
            self.channel
                .basic_publish(
                    exchange,
                    routing_key,
                    BasicPublishOptions {
                        mandatory: true,
                        ..Default::default()
                    },
                    message.payload().as_bytes(),
                    BasicProperties::default()
                        .with_content_type("application/json".into())
                        .with_delivery_mode(2)
                        .with_message_id(message.event_id().to_string().into())
                        .with_correlation_id(message.correlation_id().to_string().into()),
                )
                .await
                .map_err(publish_error)?
                .await
                .map_err(publish_error)
        };
        let confirmation = tokio::time::timeout(self.timeout, publication)
            .await
            .map_err(publish_error)??;
        if confirmation.is_ack() && confirmation.take_message().is_none() {
            return Ok(());
        }
        Err(AcolhimentoException::new(ReasonCode::IntegrationFailure))
    }
}

fn publish_error(error: impl std::error::Error + Send + Sync + 'static) -> AcolhimentoException {
    AcolhimentoException::with_source(ReasonCode::IntegrationFailure, error)
}
