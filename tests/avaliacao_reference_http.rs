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

#[tokio::test]
async fn detail_sends_patient_context_and_validates_both_returned_ids() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let id = Uuid::from_u128(1);
    let fixture = HttpFixture::start(vec![reply(detail(patient, correlation))]).await;
    let result = fixture
        .adapter
        .find_by_id(patient, id, &access(), correlation)
        .await
        .unwrap()
        .unwrap();
    assert_eq!(result.id, id);
    assert_eq!(result.patient_id, patient);
    assert!(result.titulo.is_none());
    let observed = fixture.observed().await;
    assert_eq!(
        observed[0].uri,
        format!("/api/avaliacao/pacientes/{id}?patientId={patient}")
    );
    assert_eq!(
        observed[0].authorization,
        "Bearer synthetic-delegated-token"
    );
    for (field, invalid) in [
        ("id", json!(Uuid::new_v4())),
        ("patientId", json!(Uuid::new_v4())),
        ("id", json!(Uuid::nil())),
        ("patientId", json!(Uuid::nil())),
    ] {
        let mut body = detail(patient, correlation);
        body["data"][field] = invalid;
        let fixture = HttpFixture::start(vec![reply(body)]).await;
        assert_eq!(
            fixture
                .adapter
                .find_by_id(patient, id, &access(), correlation)
                .await
                .err()
                .unwrap()
                .reason_code(),
            ReasonCode::IntegrationFailure
        );
        assert_eq!(fixture.observed().await.len(), 1);
    }
}

#[tokio::test]
async fn only_the_exact_structured_detail_not_found_is_absence() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let id = Uuid::from_u128(1);
    let mut valid = reply(missing(correlation));
    valid.status = StatusCode::NOT_FOUND;
    let fixture = HttpFixture::start(vec![valid]).await;
    assert!(
        fixture
            .adapter
            .find_by_id(patient, id, &access(), correlation)
            .await
            .unwrap()
            .is_none()
    );
    let mut route_error = missing(correlation);
    route_error["reasonCode"] = json!("ROUTE_NOT_FOUND");
    let mut wrong_status = missing(correlation);
    wrong_status["status"] = json!(200);
    for body in [
        route_error,
        wrong_status,
        missing(Uuid::new_v4()),
        json!({}),
    ] {
        let mut response = reply(body);
        response.status = StatusCode::NOT_FOUND;
        let fixture = HttpFixture::start(vec![response]).await;
        assert_eq!(
            fixture
                .adapter
                .find_by_id(patient, id, &access(), correlation)
                .await
                .err()
                .unwrap()
                .reason_code(),
            ReasonCode::IntegrationFailure
        );
    }
    let mut response = reply(missing(correlation));
    response.status = StatusCode::NOT_FOUND;
    let fixture = HttpFixture::start(vec![response]).await;
    assert_eq!(
        fixture
            .adapter
            .find_by_patient_id(patient, &access(), correlation)
            .await
            .err()
            .unwrap()
            .reason_code(),
        ReasonCode::IntegrationFailure
    );
}

#[tokio::test]
async fn a_failure_after_a_valid_first_page_never_returns_partial_data_or_retries() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    for status in [StatusCode::INTERNAL_SERVER_ERROR, StatusCode::NOT_FOUND] {
        let mut second = reply(missing(correlation));
        second.status = status;
        let fixture =
            HttpFixture::start(vec![reply(page(patient, correlation, 0, 21)), second]).await;
        assert_eq!(
            fixture
                .adapter
                .find_by_patient_id(patient, &access(), correlation)
                .await
                .err()
                .unwrap()
                .reason_code(),
            ReasonCode::IntegrationFailure
        );
        assert_eq!(fixture.observed().await.len(), 2);
    }
}

#[tokio::test]
async fn page_metadata_foreign_patients_and_duplicate_ids_are_rejected_across_pages() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let mut wrong_page = page(patient, correlation, 1, 21);
    wrong_page["data"]["page"] = json!(0);
    let mut wrong_size = page(patient, correlation, 1, 21);
    wrong_size["data"]["size"] = json!(100);
    let mut changed_total = page(patient, correlation, 1, 21);
    changed_total["data"]["total"] = json!(22);
    let mut empty_before_total = page(patient, correlation, 1, 21);
    empty_before_total["data"]["items"] = json!([]);
    let mut duplicate = page(patient, correlation, 1, 21);
    duplicate["data"]["items"][0] = reference(patient, 0);
    let mut foreign = page(patient, correlation, 1, 21);
    foreign["data"]["items"][0]["patientId"] = json!(Uuid::new_v4());
    let mut nil_id = page(patient, correlation, 1, 21);
    nil_id["data"]["items"][0]["id"] = json!(Uuid::nil());
    for second in [
        wrong_page,
        wrong_size,
        changed_total,
        empty_before_total,
        duplicate,
        foreign,
        nil_id,
    ] {
        let fixture = HttpFixture::start(vec![
            reply(page(patient, correlation, 0, 21)),
            reply(second),
        ])
        .await;
        assert_eq!(
            fixture
                .adapter
                .find_by_patient_id(patient, &access(), correlation)
                .await
                .err()
                .unwrap()
                .reason_code(),
            ReasonCode::IntegrationFailure
        );
        assert_eq!(fixture.observed().await.len(), 2);
    }
    let mut duplicate_page = page(patient, correlation, 0, 2);
    duplicate_page["data"]["items"][1] = reference(patient, 0);
    let fixture = HttpFixture::start(vec![reply(duplicate_page)]).await;
    assert_eq!(
        fixture
            .adapter
            .find_by_patient_id(patient, &access(), correlation)
            .await
            .err()
            .unwrap()
            .reason_code(),
        ReasonCode::IntegrationFailure
    );
}

#[tokio::test]
async fn response_status_mime_correlations_shape_and_one_mebibyte_limit_are_enforced() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let mut failures = Vec::new();
    for status in [
        StatusCode::CREATED,
        StatusCode::ACCEPTED,
        StatusCode::NO_CONTENT,
    ] {
        let mut response = reply(page(patient, correlation, 0, 0));
        response.status = status;
        failures.push(response);
    }
    let mut mime = reply(page(patient, correlation, 0, 0));
    mime.content_type = "application/jsonp";
    failures.push(mime);
    let mut omitted = reply(page(patient, correlation, 0, 0));
    omitted.correlation = CorrelationHeader::Omit;
    failures.push(omitted);
    let mut wrong_header = reply(page(patient, correlation, 0, 0));
    wrong_header.correlation = CorrelationHeader::Value(Uuid::new_v4().to_string());
    failures.push(wrong_header);
    failures.push(reply(page(patient, Uuid::new_v4(), 0, 0)));
    let mut wrong_code = page(patient, correlation, 0, 0);
    wrong_code["code"] = json!("OTHER_CODE");
    failures.push(reply(wrong_code));
    let mut false_success = page(patient, correlation, 0, 0);
    false_success["success"] = json!(false);
    failures.push(reply(false_success));
    let mut old_array = page(patient, correlation, 0, 0);
    old_array["data"] = json!([]);
    failures.push(reply(old_array));
    for field in ["items", "total", "page", "size"] {
        let mut missing = page(patient, correlation, 0, 0);
        missing["data"].as_object_mut().unwrap().remove(field);
        failures.push(reply(missing));
    }
    let mut malformed = reply(Value::Null);
    malformed.body = b"{".to_vec();
    failures.push(malformed);
    let mut oversized = page(patient, correlation, 0, 0);
    oversized["padding"] = json!("x".repeat(1024 * 1024));
    failures.push(reply(oversized));
    for response in failures {
        let fixture = HttpFixture::start(vec![response]).await;
        assert_eq!(
            fixture
                .adapter
                .find_by_patient_id(patient, &access(), correlation)
                .await
                .err()
                .unwrap()
                .reason_code(),
            ReasonCode::IntegrationFailure
        );
        assert_eq!(fixture.observed().await.len(), 1);
    }
}

#[tokio::test]
async fn authorization_failures_preserve_their_public_meaning_without_retry() {
    for (status, reason) in [
        (StatusCode::UNAUTHORIZED, ReasonCode::AuthenticationRequired),
        (StatusCode::FORBIDDEN, ReasonCode::AuthorizationDenied),
    ] {
        let mut response = reply(json!({}));
        response.status = status;
        response.correlation = CorrelationHeader::Omit;
        let fixture = HttpFixture::start(vec![response]).await;
        assert_eq!(
            fixture
                .adapter
                .find_by_patient_id(Uuid::new_v4(), &access(), Uuid::new_v4())
                .await
                .err()
                .unwrap()
                .reason_code(),
            reason
        );
        assert_eq!(fixture.observed().await.len(), 1);
    }
}

#[tokio::test]
async fn dependency_diagnostics_include_bounded_context_without_bearer_or_clinical_data() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let bearer_sentinel = "SYNTHETIC_BEARER_MUST_NOT_APPEAR";
    let clinical_sentinel = "SYNTHETIC_CLINICAL_CONTENT_MUST_NOT_APPEAR";
    let captured = diagnostic_capture(correlation);
    let mut second = page(patient, correlation, 1, 21);
    second["data"]["total"] = json!(clinical_sentinel);
    let fixture = HttpFixture::start(vec![
        reply(page(patient, correlation, 0, 21)),
        reply(second),
    ])
    .await;
    let result = fixture
        .adapter
        .find_by_patient_id(
            patient,
            &DelegatedAccess::new(bearer_sentinel.to_owned()),
            correlation,
        )
        .await;
    assert_eq!(
        result.err().unwrap().reason_code(),
        ReasonCode::IntegrationFailure
    );
    let all_events = captured.lock().unwrap();
    let correlation_value = correlation.to_string();
    let events: Vec<_> = all_events
        .iter()
        .filter(|event| event.get("correlation_id") == Some(&correlation_value))
        .collect();
    assert_eq!(events.len(), 1);
    let event = &events[0];
    assert_eq!(event["operation"], "find_by_patient_id");
    assert_eq!(event["phase"], "response_json");
    assert_eq!(event["outcome"], "failed");
    assert_eq!(event["page"], "1");
    assert_eq!(event["remote_status"], "200");
    assert_eq!(event["correlation_id"], correlation.to_string());
    assert_eq!(event["reason_code"], "INTEGRATION_FAILURE");
    assert!(event["duration_ms"].parse::<u128>().unwrap() < 15000);
    let diagnostics = format!("{events:?}");
    assert!(!diagnostics.contains(bearer_sentinel));
    assert!(!diagnostics.contains(clinical_sentinel));
    assert!(!diagnostics.contains("/api/avaliacao/"));
}

#[tokio::test]
async fn the_fifteen_second_budget_includes_all_pages_and_streamed_body_reads() {
    let patient = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let mut first = reply(page(patient, correlation, 0, 21));
    first.body_delay = Duration::from_secs(8);
    let mut second = reply(page(patient, correlation, 1, 21));
    second.body_delay = Duration::from_secs(8);
    let fixture =
        HttpFixture::start(vec![reply(detail(patient, correlation)), first, second]).await;
    fixture
        .adapter
        .find_by_id(patient, Uuid::from_u128(1), &access(), correlation)
        .await
        .unwrap()
        .unwrap();
    tokio::task::yield_now().await;
    tokio::time::pause();
    let _yield_guard = YieldGuard(tokio::spawn(async {
        loop {
            tokio::task::yield_now().await;
        }
    }));
    let adapter = fixture.adapter.clone();
    let started = Instant::now();
    let captured = Arc::new(std::sync::Mutex::new(Vec::new()));
    let subscriber = tracing_subscriber::registry().with(DiagnosticCapture {
        correlation_id: correlation.to_string(),
        captured: captured.clone(),
    });
    let query = tokio::spawn(
        async move {
            adapter
                .find_by_patient_id(patient, &access(), correlation)
                .await
        }
        .with_subscriber(subscriber),
    );
    assert!(fixture.wait_requests(2).await);
    tokio::time::advance(Duration::from_millis(8001)).await;
    assert!(
        fixture.wait_requests(3).await,
        "second page absent: finished={}, elapsed={:?}, diagnostics={:?}",
        query.is_finished(),
        started.elapsed(),
        captured.lock().unwrap()
    );
    tokio::time::advance(Duration::from_millis(5999)).await;
    assert!(!query.is_finished());
    tokio::time::advance(Duration::from_millis(1001)).await;
    assert_eq!(
        query.await.unwrap().err().unwrap().reason_code(),
        ReasonCode::IntegrationFailure
    );
    assert_eq!(started.elapsed(), Duration::from_millis(15001));
    let observed = fixture.observed().await;
    assert_eq!(observed.len(), 3);
    assert!(observed[0].uri.contains("?patientId="));
    assert!(observed[1].uri.ends_with("?page=0&size=20"));
    assert!(observed[2].uri.ends_with("?page=1&size=20"));
}
