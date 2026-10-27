use crate::domain::port::messaging::{OutboxEventPort, ProcessedEventPort};
use crate::infrastructure::adapter::{OutboxEventAdapter, ProcessedEventAdapter};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::persistence::repository::{
    OutboxEventRepository, ProcessedEventRepository,
};
use std::sync::Arc;

#[derive(Clone)]
pub struct MessagingServices {
    pub outbox: Arc<dyn OutboxEventPort>,
    pub processed: Arc<dyn ProcessedEventPort>,
}

impl MessagingServices {
    pub fn new(database: DatabaseConfig, max_attempts: u32) -> Self {
        Self {
            outbox: Arc::new(OutboxEventAdapter::new(OutboxEventRepository::new(
                database.clone(),
            ))),
            processed: Arc::new(ProcessedEventAdapter::new(ProcessedEventRepository::new(
                database,
                max_attempts,
            ))),
        }
    }
}
