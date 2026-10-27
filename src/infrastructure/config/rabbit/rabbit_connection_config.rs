use super::binding::RabbitAcolhimentoBindingConfig;
use super::{RabbitExchangeConfig, RabbitQueueConfig, RabbitSettings};
use crate::domain::exception::TechnicalError;
use lapin::{Channel, Connection, ConnectionProperties};
use std::time::Duration;

pub(crate) struct RabbitConnectionConfig;

impl RabbitConnectionConfig {
    pub async fn connect(settings: &RabbitSettings) -> Result<Connection, TechnicalError> {
        tokio::time::timeout(Duration::from_secs(15), Self::initialize(settings))
            .await
            .map_err(rabbit_error)?
    }

    async fn initialize(settings: &RabbitSettings) -> Result<Connection, TechnicalError> {
        let connection = Connection::connect(
            &settings.url,
            ConnectionProperties::default().with_connection_name("humanizar-acolhimento".into()),
        )
        .await
        .map_err(rabbit_error)?;
        if let Err(error) = Self::declare(&connection, settings).await {
            let _ = tokio::time::timeout(
                Duration::from_secs(5),
                connection.close(200, "Startup incompleto".into()),
            )
            .await;
            return Err(error);
        }
        Ok(connection)
    }

    async fn declare(
        connection: &Connection,
        settings: &RabbitSettings,
    ) -> Result<(), TechnicalError> {
        let channel = Self::channel(connection).await?;
        RabbitExchangeConfig::declare(&channel).await?;
        RabbitQueueConfig::declare(&channel, settings.delivery_limit).await?;
        RabbitAcolhimentoBindingConfig::declare(&channel).await?;
        channel
            .close(200, "Topologia declarada".into())
            .await
            .map_err(rabbit_error)
    }

    pub async fn channel(connection: &Connection) -> Result<Channel, TechnicalError> {
        tokio::time::timeout(Duration::from_secs(5), connection.create_channel())
            .await
            .map_err(rabbit_error)?
            .map_err(rabbit_error)
    }
}

pub(crate) fn rabbit_error(
    error: impl std::error::Error + Send + Sync + 'static,
) -> TechnicalError {
    TechnicalError::with_source("Falha de transporte RabbitMQ", error)
}
