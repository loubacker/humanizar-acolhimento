use super::rabbit_error;
use crate::application::catalog::ExchangeCatalog;
use crate::domain::exception::TechnicalError;
use lapin::options::ExchangeDeclareOptions;
use lapin::types::FieldTable;
use lapin::{Channel, ExchangeKind};

pub(crate) struct RabbitExchangeConfig;

impl RabbitExchangeConfig {
    pub async fn declare(channel: &Channel) -> Result<(), TechnicalError> {
        for exchange in [
            ExchangeCatalog::ACOLHIMENTO_COMMAND,
            ExchangeCatalog::ACOLHIMENTO_EVENT,
            ExchangeCatalog::ACOLHIMENTO_DLX,
        ] {
            channel
                .exchange_declare(
                    exchange.into(),
                    ExchangeKind::Topic,
                    ExchangeDeclareOptions {
                        durable: true,
                        ..Default::default()
                    },
                    FieldTable::default(),
                )
                .await
                .map_err(rabbit_error)?;
        }
        Ok(())
    }
}
