use super::{acolhimento_retrieve_consumer, dead_letter_consumer};
use crate::application::messaging::inbound::handler::AcolhimentoRetrieveHandler;
use crate::domain::exception::TechnicalError;
use crate::domain::port::messaging::MessagePublisherPort;
use crate::infrastructure::config::{DatabaseConfig, rabbit, rabbit::RabbitSettings};
use crate::infrastructure::messaging::MessagingServices;
use crate::infrastructure::messaging::outbound::outbox::{outbox_relay_worker, retention_worker};
use crate::infrastructure::messaging::outbound::rabbit::RabbitOutboxPublisher;
use lapin::Connection;
use std::sync::{
    Arc,
    atomic::{AtomicBool, Ordering},
};
use std::time::Duration;
use tokio::task::{JoinHandle, JoinSet};
use tokio_util::sync::CancellationToken;

pub struct RabbitRuntime {
    stop: CancellationToken,
    readiness: Arc<AtomicBool>,
    handle: JoinHandle<Result<(), TechnicalError>>,
}

impl RabbitRuntime {
    pub async fn start(
        database: DatabaseConfig,
        settings: RabbitSettings,
    ) -> Result<Self, TechnicalError> {
        let connection = database.acquire().await.map_err(|error| {
            TechnicalError::with_source("Banco da quarentena indisponivel", error)
        })?;
        connection
            .prepare("SELECT id, queue_name, exchange_name, routing_key, payload, properties, received_at FROM dead_letter_quarantine LIMIT 0")
            .await
            .map_err(|error| TechnicalError::with_source("Schema da quarentena indisponivel", error))?;
        drop(connection);
        let settings = Arc::new(settings);
        let services = MessagingServices::new(database, settings.max_attempts);
        let stop = CancellationToken::new();
        let session =
            Session::start(services.clone(), settings.clone(), stop.child_token()).await?;
        let readiness = Arc::new(AtomicBool::new(true));
        let handle = tokio::spawn(supervise(
            session,
            services,
            settings,
            stop.clone(),
            readiness.clone(),
        ));
        Ok(Self {
            stop,
            readiness,
            handle,
        })
    }

    pub fn readiness(&self) -> Arc<AtomicBool> {
        self.readiness.clone()
    }

    pub async fn shutdown(mut self) -> Result<(), TechnicalError> {
        self.stop.cancel();
        match tokio::time::timeout(Duration::from_secs(45), &mut self.handle).await {
            Ok(result) => result.map_err(|error| {
                TechnicalError::with_source("Supervisor encerrado com falha", error)
            })?,
            Err(error) => {
                self.handle.abort();
                let _ = self.handle.await;
                Err(TechnicalError::with_source(
                    "Prazo de shutdown excedido; estado pendente preservado",
                    error,
                ))
            }
        }
    }
}

struct Session {
    connection: Connection,
    tasks: JoinSet<Result<(), TechnicalError>>,
    stop: CancellationToken,
}

impl Session {
    async fn start(
        services: MessagingServices,
        settings: Arc<RabbitSettings>,
        stop: CancellationToken,
    ) -> Result<Self, TechnicalError> {
        let connection = rabbit::RabbitConnectionConfig::connect(&settings).await?;
        let mut session = Self {
            connection,
            tasks: JoinSet::new(),
            stop,
        };
        let cancellation = session.stop.clone();
        let initialized = tokio::select! {
            _ = cancellation.cancelled() => Err(TechnicalError::new("Inicializacao de workers cancelada")),
            result = tokio::time::timeout(Duration::from_secs(15), session.initialize(services, settings)) => {
                result.map_err(|error| TechnicalError::with_source("Prazo de inicializacao de workers excedido", error)).and_then(|result| result)
            }
        };
        if let Err(error) = initialized {
            session.close().await;
            return Err(error);
        }
        Ok(session)
    }

    async fn initialize(
        &mut self,
        services: MessagingServices,
        settings: Arc<RabbitSettings>,
    ) -> Result<(), TechnicalError> {
        let publisher: Arc<dyn MessagePublisherPort> = Arc::new(
            RabbitOutboxPublisher::new(
                rabbit::RabbitConnectionConfig::channel(&self.connection).await?,
                settings.confirm_timeout,
            )
            .await
            .map_err(|error| TechnicalError::with_source("Publisher nao inicializado", error))?,
        );
        let handler = Arc::new(AcolhimentoRetrieveHandler::new(services.processed.clone()));
        for index in 0..settings.consumer_count {
            let channel = rabbit::RabbitConnectionConfig::channel(&self.connection).await?;
            let consumer = acolhimento_retrieve_consumer::subscribe(
                &channel,
                settings.prefetch,
                &format!("acolhimento.retrieve.{index}"),
            )
            .await?;
            self.tasks.spawn(acolhimento_retrieve_consumer::run(
                consumer,
                handler.clone(),
                self.stop.clone(),
            ));
        }
        let consumer = dead_letter_consumer::subscribe(
            &rabbit::RabbitConnectionConfig::channel(&self.connection).await?,
        )
        .await?;
        self.tasks.spawn(dead_letter_consumer::run(
            consumer,
            services.quarantine,
            settings.reconnect_delay,
            self.stop.clone(),
        ));
        self.tasks.spawn(outbox_relay_worker::run(
            services.outbox.clone(),
            publisher,
            settings.clone(),
            self.stop.clone(),
        ));
        self.tasks.spawn(retention_worker::run(
            services.outbox,
            settings,
            self.stop.clone(),
        ));
        Ok(())
    }

    async fn close(&mut self) {
        self.stop.cancel();
        let drained = tokio::time::timeout(Duration::from_secs(30), async {
            while let Some(result) = self.tasks.join_next().await {
                if !matches!(result, Ok(Ok(()))) {
                    tracing::warn!(
                        service = "humanizar-acolhimento",
                        phase = "worker_drain",
                        outcome = "failure"
                    );
                }
            }
        })
        .await;
        if drained.is_err() {
            self.tasks.abort_all();
            while self.tasks.join_next().await.is_some() {}
            tracing::warn!(
                service = "humanizar-acolhimento",
                phase = "worker_drain",
                outcome = "timeout"
            );
        }
        let _ = tokio::time::timeout(
            Duration::from_secs(5),
            self.connection.close(200, "Encerramento".into()),
        )
        .await;
    }
}

async fn supervise(
    mut session: Session,
    services: MessagingServices,
    settings: Arc<RabbitSettings>,
    stop: CancellationToken,
    readiness: Arc<AtomicBool>,
) -> Result<(), TechnicalError> {
    loop {
        tokio::select! {
            _ = stop.cancelled() => { readiness.store(false, Ordering::Release); session.close().await; return Ok(()); }
            _ = session.tasks.join_next() => { readiness.store(false, Ordering::Release); session.close().await; }
        }
        tracing::error!(
            service = "humanizar-acolhimento",
            phase = "messaging_supervisor",
            outcome = "restarting"
        );
        loop {
            tokio::select! { _ = stop.cancelled() => return Ok(()), _ = tokio::time::sleep(settings.reconnect_delay) => {} }
            let creation =
                Session::start(services.clone(), settings.clone(), stop.child_token()).await;
            match creation {
                Ok(next) => {
                    session = next;
                    readiness.store(true, Ordering::Release);
                    break;
                }
                Err(_) => tracing::warn!(
                    service = "humanizar-acolhimento",
                    phase = "messaging_reconnect",
                    outcome = "failure"
                ),
            }
        }
    }
}
