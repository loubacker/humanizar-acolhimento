use crate::application::catalog::{ExchangeCatalog, QueueCatalog, RoutingKeyCatalog};
use crate::domain::exception::TechnicalError;
use crate::infrastructure::config::rabbit::rabbit_error;
use lapin::Channel;
use lapin::options::QueueBindOptions;
use lapin::types::FieldTable;

pub(crate) struct RabbitAcolhimentoBindingConfig;

impl RabbitAcolhimentoBindingConfig {
    pub async fn declare(channel: &Channel) -> Result<(), TechnicalError> {
        channel
            .queue_bind(
                QueueCatalog::PATIENT_RETRIEVE.into(),
                ExchangeCatalog::ACOLHIMENTO_COMMAND.into(),
                RoutingKeyCatalog::PATIENT_RETRIEVE_V1.into(),
                QueueBindOptions::default(),
                FieldTable::default(),
            )
            .await
            .map_err(rabbit_error)
    }
}
