use std::collections::HashMap;
use std::future::{Future, poll_fn};
use std::hash::Hash;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex};
use std::time::Duration;

use moka::future::Cache;
use tokio::task::JoinHandle;
use tokio::time::Instant;
use tokio_postgres::{AsyncMessage, Config, NoTls};
use uuid::Uuid;

pub const RECORDS_CAPACITY: u64 = 16 * 1024 * 1024;
pub const SEARCH_CAPACITY: u64 = 4 * 1024 * 1024;
pub const CACHE_TTL: Duration = Duration::from_secs(604_800);
const DISCONNECTED: u64 = 1;
const GENERATION_STEP: u64 = 2;
const CONNECTION_DEADLINE: Duration = Duration::from_secs(5);
const HEARTBEAT_INTERVAL: Duration = Duration::from_secs(10);
const RECONNECT_INTERVAL: Duration = Duration::from_secs(5);
const METRICS_INTERVAL: Duration = Duration::from_secs(60);
const ENTRY_OVERHEAD: u32 = 64;
const MAX_SCOPES: usize = 4096;
const MIGRATION_CHECK: &str = "
    SELECT COUNT(*) >= $1
    FROM pg_catalog.pg_trigger AS entry
    JOIN pg_catalog.pg_class AS relation ON relation.oid = entry.tgrelid
    JOIN pg_catalog.pg_namespace AS schema_info ON schema_info.oid = relation.relnamespace
    WHERE schema_info.nspname = current_schema()
        AND entry.tgfoid = to_regprocedure('public.notify_cadastral_cache_change()')
        AND entry.tgenabled IN ('O', 'A')
";

struct CacheObserver {
    clear: Box<dyn Fn() + Send + Sync>,
    weighted_size: Box<dyn Fn() -> u64 + Send + Sync>,
}

#[derive(Default)]
struct ScopeVersions {
    reset: u64,
    entries: HashMap<Uuid, u64>,
}

pub struct CadastralCacheState {
    generation: AtomicU64,
    scope_versions: Mutex<ScopeVersions>,
    observers: Mutex<Vec<CacheObserver>>,
    hits: AtomicU64,
    misses: AtomicU64,
    bypasses: AtomicU64,
    invalidations: AtomicU64,
}

impl CadastralCacheState {
    pub fn new() -> Arc<Self> {
        Arc::new(Self {
            generation: AtomicU64::new(DISCONNECTED),
            scope_versions: Mutex::new(ScopeVersions::default()),
            observers: Mutex::new(Vec::new()),
            hits: AtomicU64::new(0),
            misses: AtomicU64::new(0),
            bypasses: AtomicU64::new(0),
            invalidations: AtomicU64::new(0),
        })
    }

    pub fn invalidate(&self) {
        {
            let mut versions = self.lock_scope_versions();
            versions.reset = self.advance_generation();
            versions.entries.clear();
        }
        self.invalidations.fetch_add(1, Ordering::Relaxed);
        let observers = self
            .observers
            .lock()
            .unwrap_or_else(|error| error.into_inner());
        for observer in observers.iter() {
            (observer.clear)();
        }
    }

    pub fn invalidate_scope(&self, scope: Uuid) {
        let mut versions = self.lock_scope_versions();
        if versions.entries.len() >= MAX_SCOPES && !versions.entries.contains_key(&scope) {
            drop(versions);
            self.invalidate();
            return;
        }
        versions.entries.insert(scope, self.advance_generation());
        self.invalidations.fetch_add(1, Ordering::Relaxed);
    }

    fn lock_scope_versions(&self) -> std::sync::MutexGuard<'_, ScopeVersions> {
        self.scope_versions
            .lock()
            .unwrap_or_else(|error| error.into_inner())
    }

    fn advance_generation(&self) -> u64 {
        self.generation
            .fetch_add(GENERATION_STEP, Ordering::AcqRel)
            .wrapping_add(GENERATION_STEP)
    }

    fn stamp(&self, generation: u64, scope: Option<Uuid>) -> Option<u64> {
        let versions = self.lock_scope_versions();
        if self.generation() != Some(generation) {
            return None;
        }
        Some(scope.map_or(generation, |id| {
            versions.entries.get(&id).copied().unwrap_or(versions.reset)
        }))
    }

    pub fn start(
        self: Arc<Self>,
        config: Config,
        channel: &'static str,
        expected_triggers: i64,
    ) -> CadastralCacheListener {
        let state = Arc::clone(&self);
        let task = tokio::spawn(async move {
            let _availability = ListenerAvailability(&state);
            loop {
                let reason = state.listen(&config, channel, expected_triggers).await;
                state.disconnect();
                tracing::warn!(channel, reason, "Cache de leitura em leitura direta");
                tokio::time::sleep(RECONNECT_INTERVAL).await;
            }
        });

        CadastralCacheListener { state: self, task }
    }

    pub fn generation(&self) -> Option<u64> {
        let generation = self.generation.load(Ordering::Acquire);
        (generation & DISCONNECTED == 0).then_some(generation)
    }

    fn connect(&self) {
        self.invalidate();
        self.generation.fetch_and(!DISCONNECTED, Ordering::Release);
    }

    fn disconnect(&self) {
        let previous = self.generation.fetch_or(DISCONNECTED, Ordering::AcqRel);
        if previous & DISCONNECTED == 0 {
            self.invalidate();
        }
    }

    async fn listen(
        &self,
        config: &Config,
        channel: &'static str,
        expected_triggers: i64,
    ) -> &'static str {
        if channel.is_empty()
            || !channel
                .bytes()
                .all(|byte| byte.is_ascii_lowercase() || byte == b'_')
        {
            return "INVALID_LISTEN_CHANNEL";
        }

        let Ok(Ok((client, mut connection))) =
            tokio::time::timeout(CONNECTION_DEADLINE, config.connect(NoTls)).await
        else {
            return "LISTENER_CONNECTION_UNAVAILABLE";
        };

        let driver = async {
            while let Some(message) = poll_fn(|context| connection.poll_message(context)).await {
                match message {
                    Ok(AsyncMessage::Notification(notification))
                        if notification.channel() == channel =>
                    {
                        match Uuid::parse_str(notification.payload()) {
                            Ok(scope) => self.invalidate_scope(scope),
                            Err(_) => self.invalidate(),
                        }
                    }
                    Err(_) => return "LISTENER_CONNECTION_LOST",
                    _ => {}
                }
            }

            "LISTENER_CONNECTION_CLOSED"
        };

        let subscription = async {
            let setup = async {
                let migration = client
                    .query_one(MIGRATION_CHECK, &[&expected_triggers])
                    .await?;
                if !migration.try_get::<_, bool>(0)? {
                    return Ok::<_, tokio_postgres::Error>(false);
                }

                client.batch_execute(&format!("LISTEN {channel}")).await?;
                Ok(true)
            };

            match tokio::time::timeout(CONNECTION_DEADLINE, setup).await {
                Ok(Ok(true)) => {}
                Ok(Ok(false)) => return "LISTENER_MIGRATION_PENDING",
                _ => return "LISTENER_SUBSCRIPTION_UNAVAILABLE",
            }

            self.connect();
            tracing::info!(channel, "Cache de leitura conectado");
            let mut last_metrics = tokio::time::Instant::now();
            loop {
                tokio::time::sleep(HEARTBEAT_INTERVAL).await;
                let result = tokio::time::timeout(
                    CONNECTION_DEADLINE,
                    client.query_one(MIGRATION_CHECK, &[&expected_triggers]),
                )
                .await;
                match result {
                    Ok(Ok(row)) if matches!(row.try_get::<_, bool>(0), Ok(true)) => {}
                    Ok(Ok(_)) => return "LISTENER_MIGRATION_PENDING",
                    _ => return "LISTENER_HEARTBEAT_UNAVAILABLE",
                }

                if last_metrics.elapsed() >= METRICS_INTERVAL {
                    self.log_metrics(channel);
                    last_metrics = tokio::time::Instant::now();
                }
            }
        };

        tokio::select! {
            reason = driver => reason,
            reason = subscription => reason,
        }
    }

    fn log_metrics(&self, channel: &'static str) {
        let observers = self
            .observers
            .lock()
            .unwrap_or_else(|error| error.into_inner());
        let weighted_bytes: u64 = observers
            .iter()
            .map(|observer| (observer.weighted_size)())
            .sum();

        tracing::debug!(
            channel,
            hits = self.hits.load(Ordering::Relaxed),
            misses = self.misses.load(Ordering::Relaxed),
            bypasses = self.bypasses.load(Ordering::Relaxed),
            invalidations = self.invalidations.load(Ordering::Relaxed),
            weighted_bytes,
            "Métricas do cache de leitura"
        );
    }
}

struct ListenerAvailability<'a>(&'a CadastralCacheState);

impl Drop for ListenerAvailability<'_> {
    fn drop(&mut self) {
        self.0.disconnect();
    }
}

pub struct CadastralCacheListener {
    state: Arc<CadastralCacheState>,
    task: JoinHandle<()>,
}

impl CadastralCacheListener {
    pub fn generation(&self) -> Option<u64> {
        self.state.generation()
    }
}

impl Drop for CadastralCacheListener {
    fn drop(&mut self) {
        self.task.abort();
        self.state.disconnect();
    }
}

#[derive(Clone)]
struct CacheEntry<V> {
    value: V,
    expires_at: Instant,
}

type ScopeOf<K> = Arc<dyn Fn(&K) -> Uuid + Send + Sync>;

#[derive(Clone)]
pub struct CadastralCache<K, V> {
    state: Arc<CadastralCacheState>,
    scope_of: Option<ScopeOf<K>>,
    entries: Cache<(u64, K), CacheEntry<V>>,
}

impl<K, V> CadastralCache<K, V>
where
    K: Clone + Eq + Hash + Send + Sync + 'static,
    V: Clone + Send + Sync + 'static,
{
    pub fn new(
        state: Arc<CadastralCacheState>,
        capacity: u64,
        weight: impl Fn(&K, &V) -> u32 + Send + Sync + 'static,
    ) -> Self {
        Self::build(state, capacity, None, weight)
    }

    pub fn new_scoped(
        state: Arc<CadastralCacheState>,
        capacity: u64,
        scope_of: impl Fn(&K) -> Uuid + Send + Sync + 'static,
        weight: impl Fn(&K, &V) -> u32 + Send + Sync + 'static,
    ) -> Self {
        Self::build(state, capacity, Some(Arc::new(scope_of)), weight)
    }

    fn build(
        state: Arc<CadastralCacheState>,
        capacity: u64,
        scope_of: Option<ScopeOf<K>>,
        weight: impl Fn(&K, &V) -> u32 + Send + Sync + 'static,
    ) -> Self {
        let entries = Cache::<(u64, K), CacheEntry<V>>::builder()
            .max_capacity(capacity)
            .weigher(move |(_, key), value| {
                weight(key, &value.value).saturating_add(ENTRY_OVERHEAD)
            })
            .time_to_live(CACHE_TTL)
            .build();
        let invalidation = entries.clone();
        let measurement = entries.clone();
        state
            .observers
            .lock()
            .unwrap_or_else(|error| error.into_inner())
            .push(CacheObserver {
                clear: Box::new(move || invalidation.invalidate_all()),
                weighted_size: Box::new(move || measurement.weighted_size()),
            });

        Self {
            state,
            scope_of,
            entries,
        }
    }

    pub fn generation(&self) -> Option<u64> {
        let generation = self.state.generation();
        if generation.is_none() {
            self.state.bypasses.fetch_add(1, Ordering::Relaxed);
        }

        generation
    }

    pub async fn get_or_load<E>(
        &self,
        key: K,
        load: impl Future<Output = Result<V, E>>,
    ) -> Result<V, E> {
        let Some(generation) = self.generation() else {
            return load.await;
        };
        if let Some(value) = self.get(generation, &key).await {
            return Ok(value);
        }

        let value = load.await?;
        self.insert(generation, key, value.clone()).await;
        Ok(value)
    }

    pub async fn get(&self, generation: u64, key: &K) -> Option<V> {
        let scope = self.scope_of.as_ref().map(|scope_of| scope_of(key));
        let stamp = self.state.stamp(generation, scope)?;
        let value = self
            .entries
            .get(&(stamp, key.clone()))
            .await
            .filter(|entry| Instant::now() < entry.expires_at)
            .map(|entry| entry.value);
        if self.state.generation() == Some(generation) && value.is_some() {
            self.state.hits.fetch_add(1, Ordering::Relaxed);
            return value;
        }

        self.state.misses.fetch_add(1, Ordering::Relaxed);
        None
    }

    pub async fn insert(&self, generation: u64, key: K, value: V) {
        let scope = self.scope_of.as_ref().map(|scope_of| scope_of(&key));
        let Some(stamp) = self.state.stamp(generation, scope) else {
            return;
        };

        let key = (stamp, key);
        self.entries
            .insert(
                key.clone(),
                CacheEntry {
                    value,
                    expires_at: Instant::now() + CACHE_TTL,
                },
            )
            .await;
        let latest = self
            .state
            .generation()
            .and_then(|generation| self.state.stamp(generation, scope));
        if latest != Some(stamp) {
            self.entries.invalidate(&key).await;
        }
    }
}
