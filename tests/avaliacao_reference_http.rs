#![forbid(unsafe_code)]

use axum::Router;
use axum::body::Body;
use axum::extract::State;
use axum::http::{HeaderMap, Response, StatusCode, Uri};
use axum::routing::get;
use humanizar_acolhimento::domain::model::DelegatedAccess;
use humanizar_acolhimento::domain::model::enums::ReasonCode;
use humanizar_acolhimento::domain::port::AvaliacaoPacienteReferencePort;
use humanizar_acolhimento::infrastructure::AvaliacaoPacienteReferenceAdapter;
use humanizar_acolhimento::infrastructure::config::AvaliacaoPacienteSettings;
use serde_json::{Value, json};
use std::collections::{BTreeMap, VecDeque};
use std::sync::{Arc, OnceLock};
use std::time::Duration;
use tokio::sync::Mutex;
use tokio::task::JoinHandle;
use tokio::time::{Instant, sleep_until};
use tracing::field::{Field, Visit};
use tracing::instrument::WithSubscriber;
use tracing::{Event, Subscriber};
use tracing_subscriber::layer::{Context, Layer};
use tracing_subscriber::prelude::*;
use uuid::Uuid;

enum CorrelationHeader {
    Echo,
    Omit,
    Value(String),
}

struct Reply {
    status: StatusCode,
    content_type: &'static str,
    correlation: CorrelationHeader,
    body: Vec<u8>,
    body_delay: Duration,
}

#[derive(Clone)]
struct ObservedRequest {
    uri: String,
    authorization: String,
    correlation: String,
}

#[derive(Clone)]
struct FixtureState {
    replies: Arc<Mutex<VecDeque<Reply>>>,
    observed: Arc<Mutex<Vec<ObservedRequest>>>,
}

struct HttpFixture {
    adapter: Arc<AvaliacaoPacienteReferenceAdapter>,
    state: FixtureState,
    task: JoinHandle<()>,
}

impl HttpFixture {
    async fn start(replies: Vec<Reply>) -> Self {
        let state = FixtureState {
            replies: Arc::new(Mutex::new(replies.into())),
            observed: Arc::new(Mutex::new(Vec::new())),
        };
        let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
        let address = listener.local_addr().unwrap();
        let router = Router::new()
            .route("/{*path}", get(respond))
            .with_state(state.clone());
        let task = tokio::spawn(async move {
            axum::serve(listener, router).await.unwrap();
        });
        let adapter = Arc::new(
            AvaliacaoPacienteReferenceAdapter::new(
                AvaliacaoPacienteSettings::new(format!("http://{address}")).unwrap(),
            )
            .unwrap(),
        );
        Self {
            adapter,
            state,
            task,
        }
    }

    async fn observed(&self) -> Vec<ObservedRequest> {
        self.state.observed.lock().await.clone()
    }

    async fn wait_requests(&self, count: usize) -> bool {
        for _ in 0..100000 {
            if self.state.observed.lock().await.len() >= count {
                return true;
            }
            tokio::task::yield_now().await;
        }
        false
    }
}

impl Drop for HttpFixture {
    fn drop(&mut self) {
        self.task.abort();
    }
}

struct YieldGuard(JoinHandle<()>);

impl Drop for YieldGuard {
    fn drop(&mut self) {
        self.0.abort();
    }
}

struct DiagnosticCapture {
    correlation_id: String,
    captured: Arc<std::sync::Mutex<Vec<BTreeMap<String, String>>>>,
}

fn diagnostic_capture(
    correlation_id: Uuid,
) -> Arc<std::sync::Mutex<Vec<BTreeMap<String, String>>>> {
    static CAPTURED: OnceLock<Arc<std::sync::Mutex<Vec<BTreeMap<String, String>>>>> =
        OnceLock::new();
    CAPTURED
        .get_or_init(|| {
            let captured = Arc::new(std::sync::Mutex::new(Vec::new()));
            tracing::subscriber::set_global_default(tracing_subscriber::registry().with(
                DiagnosticCapture {
                    correlation_id: correlation_id.to_string(),
                    captured: captured.clone(),
                },
            ))
            .unwrap();
            captured
        })
        .clone()
}

#[derive(Default)]
struct CapturedFields(BTreeMap<String, String>);

impl Visit for CapturedFields {
    fn record_debug(&mut self, field: &Field, value: &dyn std::fmt::Debug) {
        self.0.insert(field.name().to_owned(), format!("{value:?}"));
    }

    fn record_str(&mut self, field: &Field, value: &str) {
        self.0.insert(field.name().to_owned(), value.to_owned());
    }
}

impl<S: Subscriber> Layer<S> for DiagnosticCapture {
    fn on_event(&self, event: &Event<'_>, _context: Context<'_, S>) {
        if !event
            .metadata()
            .target()
            .ends_with("avaliacao_paciente_reference_adapter")
        {
            return;
        }
        let mut fields = CapturedFields::default();
        event.record(&mut fields);
        if fields.0.get("correlation_id") != Some(&self.correlation_id) {
            return;
        }
        self.captured.lock().unwrap().push(fields.0);
    }
}

async fn respond(
    State(state): State<FixtureState>,
    uri: Uri,
    headers: HeaderMap,
) -> Response<Body> {
    let accepted = Instant::now();
    let correlation = headers
        .get("X-Correlation-ID")
        .and_then(|value| value.to_str().ok())
        .unwrap_or_default()
        .to_owned();
    state.observed.lock().await.push(ObservedRequest {
        uri: uri.to_string(),
        authorization: headers
            .get("Authorization")
            .and_then(|value| value.to_str().ok())
            .unwrap_or_default()
            .to_owned(),
        correlation: correlation.clone(),
    });
    let Some(reply) = state.replies.lock().await.pop_front() else {
        return Response::builder()
            .status(StatusCode::INTERNAL_SERVER_ERROR)
            .body(Body::empty())
            .unwrap();
    };
    let mut response = Response::builder()
        .status(reply.status)
        .header("Content-Type", reply.content_type);
    response = match reply.correlation {
        CorrelationHeader::Echo => response.header("X-Correlation-ID", correlation),
        CorrelationHeader::Omit => response,
        CorrelationHeader::Value(value) => response.header("X-Correlation-ID", value),
    };
    let deadline = accepted + reply.body_delay;
    response
        .body(Body::from_stream(futures_util::stream::once(async move {
            sleep_until(deadline).await;
            Ok::<_, std::io::Error>(reply.body)
        })))
        .unwrap()
}

fn reply(body: Value) -> Reply {
    Reply {
        status: StatusCode::OK,
        content_type: "application/json",
        correlation: CorrelationHeader::Echo,
        body: serde_json::to_vec(&body).unwrap(),
        body_delay: Duration::ZERO,
    }
}

fn reference(patient: Uuid, index: u128) -> Value {
    let modality = ["RELATORIO_TECNICO", "PROFISSIONAL", "FAMILIAR"][(index % 3) as usize];
    let status = ["PENDENTE", "EM_ANDAMENTO", "CONCLUIDO"][(index % 3) as usize];
    json!({
        "id": Uuid::from_u128(index + 1), "patientId": patient,
        "tituloAvaliacao": if index == 0 { Value::Null } else { json!(format!("Título {index}")) },
        "modalidade": modality,
        "status": status,
        "statusRequest": if index == 2 { "DELETED" } else { "UPDATED" },
        "revision": 5, "respostas": [], "documento": null
    })
}

fn page(patient: Uuid, correlation: Uuid, index: u32, total: u64) -> Value {
    let offset = u64::from(index) * 20;
    let count = total.saturating_sub(offset).min(20);
    let items: Vec<_> = (offset..offset + count)
        .map(|value| reference(patient, u128::from(value)))
        .collect();
    json!({"code":"AVALIACOES_PATIENT_LISTED","success":true,"correlationId":correlation,
        "data":{"items":items,"total":total,"page":index,"size":20}})
}

fn detail(patient: Uuid, correlation: Uuid) -> Value {
    json!({"code":"AVALIACAO_FOUND","success":true,"correlationId":correlation,
        "data":reference(patient,0)})
}

fn missing(correlation: Uuid) -> Value {
    json!({"status":404,"reasonCode":"AVALIACAO_NOT_FOUND","correlationId":correlation})
}

fn access() -> DelegatedAccess {
    DelegatedAccess::new("synthetic-delegated-token".to_owned())
}

#[tokio::test]
async fn reads_all_pages_in_order_without_filtering_modes_states_or_nullable_titles() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let fixture = HttpFixture::start(
        (0..3)
            .map(|index| reply(page(patient, correlation, index, 41)))
            .collect(),
    )
    .await;
    let values = fixture
        .adapter
        .find_by_patient_id(patient, &access(), correlation)
        .await
        .unwrap();
    assert_eq!(values.len(), 41);
    assert!(values[0].titulo.is_none());
    for (index, value) in values.iter().enumerate() {
        assert_eq!(value.id, Uuid::from_u128(index as u128 + 1));
        assert_eq!(value.patient_id, patient);
        assert_eq!(
            value.modalidade,
            reference(patient, index as u128)["modalidade"]
        );
        assert_eq!(value.status, reference(patient, index as u128)["status"]);
    }
    let observed = fixture.observed().await;
    assert_eq!(observed.len(), 3);
    for (index, request) in observed.iter().enumerate() {
        assert_eq!(
            request.uri,
            format!("/api/avaliacao/pacientes/paciente/{patient}?page={index}&size=20")
        );
        assert_eq!(request.authorization, "Bearer synthetic-delegated-token");
        assert_eq!(request.correlation, correlation.to_string());
    }
}

#[tokio::test]
async fn zero_total_returns_an_empty_array_after_one_valid_page() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let fixture = HttpFixture::start(vec![reply(page(patient, correlation, 0, 0))]).await;
    assert!(
        fixture
            .adapter
            .find_by_patient_id(patient, &access(), correlation)
            .await
            .unwrap()
            .is_empty()
    );
    assert_eq!(fixture.observed().await.len(), 1);
}

