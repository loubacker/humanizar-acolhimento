use crate::domain::exception::TechnicalError;
use crate::infrastructure::config::EnvironmentConfig;
use crate::infrastructure::messaging::outbound::outbox::OutboxRetryPolicy;
use std::time::Duration;
use url::Url;

pub struct RabbitSettings {
    pub(crate) url: String,
    pub(crate) prefetch: u16,
    pub(crate) consumer_count: u32,
    pub(crate) delivery_limit: i32,
    pub(crate) confirm_timeout: Duration,
    pub(crate) reconnect_delay: Duration,
    pub(crate) batch_size: u32,
    pub(crate) parallelism: usize,
    pub(crate) poll_interval: Duration,
    pub(crate) lease: Duration,
    pub(crate) max_attempts: u32,
    pub(crate) retry_policy: OutboxRetryPolicy,
    pub(crate) retention_interval: Duration,
    pub(crate) outbox_retention: chrono::Duration,
    pub(crate) processed_retention: chrono::Duration,
}

impl RabbitSettings {
    pub fn from_env() -> Result<Self, TechnicalError> {
        Self::new(EnvironmentConfig::required("RABBITMQ_URL")?)?.with_environment_policy()
    }

    pub fn new(url: String) -> Result<Self, TechnicalError> {
        let parsed = Url::parse(&url)
            .map_err(|error| TechnicalError::with_source("RABBITMQ_URL invalida", error))?;
        if !matches!(parsed.scheme(), "amqp" | "amqps") || parsed.host_str().is_none() {
            return Err(TechnicalError::new(
                "RABBITMQ_URL deve usar amqp ou amqps com host",
            ));
        }
        Ok(Self {
            url,
            prefetch: 20,
            consumer_count: 2,
            delivery_limit: 3,
            confirm_timeout: Duration::from_secs(5),
            reconnect_delay: Duration::from_secs(5),
            batch_size: 20,
            parallelism: 8,
            poll_interval: Duration::from_millis(5000),
            lease: Duration::from_secs(300),
            max_attempts: 5,
            retry_policy: OutboxRetryPolicy::default(),
            retention_interval: Duration::from_secs(3600),
            outbox_retention: chrono::Duration::hours(48),
            processed_retention: chrono::Duration::days(90),
        })
    }

    fn with_environment_policy(mut self) -> Result<Self, TechnicalError> {
        self.prefetch = u16::try_from(EnvironmentConfig::positive_u32(
            "RABBITMQ_PREFETCH_COUNT",
            20,
        )?)
        .map_err(|error| TechnicalError::with_source("Prefetch invalido", error))?;
        self.consumer_count = EnvironmentConfig::positive_u32("RABBITMQ_CONSUMER_COUNT", 2)?;
        self.delivery_limit = i32::try_from(EnvironmentConfig::positive_u32(
            "RABBITMQ_DELIVERY_LIMIT",
            3,
        )?)
        .map_err(|error| TechnicalError::with_source("Delivery limit invalido", error))?;
        self.confirm_timeout = EnvironmentConfig::positive_duration_seconds(
            "RABBITMQ_PUBLISH_CONFIRM_TIMEOUT_SECONDS",
            5,
        )?;
        self.reconnect_delay =
            EnvironmentConfig::positive_duration_seconds("RABBITMQ_RECONNECT_DELAY_SECONDS", 5)?;
        self.batch_size = EnvironmentConfig::positive_u32("OUTBOX_BATCH_SIZE", 20)?;
        self.parallelism = EnvironmentConfig::positive_u32("OUTBOX_PARALLELISM", 8)? as usize;
        self.poll_interval =
            EnvironmentConfig::positive_duration_millis("OUTBOX_POLL_INTERVAL_MS", 5000)?;
        self.lease = EnvironmentConfig::positive_duration_seconds("OUTBOX_LOCK_SECONDS", 300)?;
        self.max_attempts = EnvironmentConfig::positive_u32("OUTBOX_MAX_ATTEMPTS", 5)?;
        self.retry_policy = OutboxRetryPolicy::new(
            EnvironmentConfig::positive_duration_millis("OUTBOX_RETRY_INITIAL_MS", 2000)?,
            EnvironmentConfig::positive_duration_millis("OUTBOX_RETRY_MAX_MS", 300000)?,
        )?;
        self.retention_interval =
            EnvironmentConfig::positive_duration_seconds("RETENTION_INTERVAL_SECONDS", 3600)?;
        self.outbox_retention = chrono::Duration::hours(i64::from(
            EnvironmentConfig::positive_u32("OUTBOX_RETENTION_HOURS", 48)?,
        ));
        self.processed_retention = chrono::Duration::days(i64::from(
            EnvironmentConfig::positive_u32("PROCESSED_RETENTION_DAYS", 90)?,
        ));
        if self.outbox_retention > chrono::Duration::days(36500)
            || self.processed_retention > chrono::Duration::days(36500)
        {
            return Err(TechnicalError::new("Retencao excede 100 anos"));
        }
        if self.consumer_count > 8 || self.parallelism > 8 || self.max_attempts > i32::MAX as u32 {
            return Err(TechnicalError::new(
                "Concorrencia maxima 8; OUTBOX_MAX_ATTEMPTS deve caber em INTEGER",
            ));
        }
        let waves = u64::from(self.batch_size).div_ceil(self.parallelism as u64);
        if self
            .confirm_timeout
            .checked_mul(u32::try_from(waves).unwrap_or(u32::MAX))
            .is_none_or(|minimum| minimum >= self.lease)
        {
            return Err(TechnicalError::new(
                "Lease deve exceder as ondas de publicacao do batch",
            ));
        }
        Ok(self)
    }
}
