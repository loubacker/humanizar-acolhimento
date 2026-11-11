#![forbid(unsafe_code)]

use std::collections::HashMap;
use std::sync::{Arc, atomic::AtomicBool};

use axum::{
    Json, Router,
    body::Body,
    extract::Path,
    http::{HeaderMap, Request, Response, StatusCode},
    response::IntoResponse,
    routing::get,
};
use base64::{Engine, engine::general_purpose::URL_SAFE_NO_PAD};
use humanizar_acolhimento::application::catalog::QueueCatalog;
use humanizar_acolhimento::infrastructure::config::rabbit::RabbitSettings;
use humanizar_acolhimento::infrastructure::config::{
    AvaliacaoPacienteSettings, DatabaseConfig, DatabaseSettings, SecurityConfig, SecuritySettings,
};
use humanizar_acolhimento::infrastructure::controller::{ApplicationState, create_router};
use humanizar_acolhimento::infrastructure::messaging::inbound::rabbit::RabbitRuntime;
use jsonwebtoken::{Algorithm, EncodingKey, Header};
use rsa::{
    RsaPrivateKey,
    pkcs8::{EncodePrivateKey, LineEnding},
    rand_core::OsRng,
    traits::PublicKeyParts,
};
use serde_json::{Value, json};
use tower::ServiceExt;
use tracing::{
    Event, Subscriber,
    field::{Field, Visit},
};
use tracing_subscriber::{Layer, layer::Context, prelude::*};
use uuid::Uuid;

struct QuarantineRetryLayer(Arc<std::sync::atomic::AtomicUsize>);

#[derive(Default)]
struct EventPhase(Option<String>);

impl Visit for EventPhase {
    fn record_debug(&mut self, field: &Field, value: &dyn std::fmt::Debug) {
        if field.name() == "phase" {
            self.0 = Some(format!("{value:?}").trim_matches('"').to_owned());
        }
    }

    fn record_str(&mut self, field: &Field, value: &str) {
        if field.name() == "phase" {
            self.0 = Some(value.to_owned());
        }
    }
}

impl<S: Subscriber> Layer<S> for QuarantineRetryLayer {
    fn on_event(&self, event: &Event<'_>, _context: Context<'_, S>) {
        let mut phase = EventPhase::default();
        event.record(&mut phase);
        if phase.0.as_deref() == Some("dead_letter_quarantine") {
            self.0.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        }
    }
}

async fn database() -> DatabaseConfig {
    let url = std::env::var("ACOLHIMENTO_TEST_DATABASE_URL").expect("URL de teste ausente");
    let parsed = url::Url::parse(&url).unwrap();
    assert_eq!(parsed.host_str(), Some("127.0.0.1"));
    assert_eq!(parsed.path(), "/acolhimento_isolated_test");
    let password = std::env::var("ACOLHIMENTO_TEST_DATABASE_PASSWORD")
        .unwrap_or_else(|_| "local-test-2026".to_owned());
    DatabaseConfig::initialize(DatabaseSettings::new(url, "postgres", password).unwrap())
        .await
        .unwrap()
}

async fn router(
    database: DatabaseConfig,
) -> (
    Router,
    String,
    String,
    String,
    Uuid,
    tokio::task::JoinHandle<()>,
    Arc<tokio::sync::RwLock<HashMap<Uuid, Value>>>,
    Arc<tokio::sync::Mutex<Vec<String>>>,
) {
    let private_key = RsaPrivateKey::new(&mut OsRng, 2048).unwrap();
    let jwks = json!({"keys": [{
        "kty": "RSA", "use": "sig", "alg": "RS256", "kid": "test-key",
        "n": URL_SAFE_NO_PAD.encode(private_key.n().to_bytes_be()),
        "e": URL_SAFE_NO_PAD.encode(private_key.e().to_bytes_be()),
    }]});
    let listener = tokio::net::TcpListener::bind("127.0.0.1:0").await.unwrap();
    let address = listener.local_addr().unwrap();
    let references = Arc::new(tokio::sync::RwLock::new(HashMap::<Uuid, Value>::new()));
    let observed_tokens = Arc::new(tokio::sync::Mutex::new(Vec::<String>::new()));
    let fixture_references = references.clone();
    let fixture_tokens = observed_tokens.clone();
    let fixture_list_references = references.clone();
    let fixture_list_tokens = observed_tokens.clone();
    let task = tokio::spawn(async move {
        axum::serve(
            listener,
            Router::new().route("/jwks", get(move || async move { Json(jwks) }))
                .route("/api/avaliacao/pacientes/paciente/{patientId}", get(move |Path(patient_id): Path<Uuid>, headers: HeaderMap| {
                    let references = fixture_list_references.clone();
                    let tokens = fixture_list_tokens.clone();
                    async move {
                        let token = headers.get("authorization").and_then(|value| value.to_str().ok()).unwrap_or_default().to_owned();
                        tokens.lock().await.push(token);
                        let values: Vec<Value> = references.read().await.values()
                            .filter(|value| value["patientId"] == patient_id.to_string()).cloned().collect();
                        if values.iter().any(|value| value["fixtureListNotFound"] == true) {
                            return (StatusCode::NOT_FOUND, Json(json!({
                                "status":404, "reasonCode":"ROUTE_NOT_FOUND",
                                "correlationId":headers.get("x-correlation-id").and_then(|value| value.to_str().ok()).unwrap_or_default()
                            }))).into_response();
                        }
                        Json(json!({
                            "code":"AVALIACOES_PATIENT_LISTED", "success":true,
                            "correlationId":headers.get("x-correlation-id").and_then(|value| value.to_str().ok()).unwrap_or_default(),
                            "data":values
                        })).into_response()
                    }
                }))
                .route("/api/avaliacao/pacientes/{id}", get(move |Path(id): Path<Uuid>, headers: HeaderMap| {
                    let references = fixture_references.clone();
                    let tokens = fixture_tokens.clone();
                    async move {
                        let token = headers.get("authorization").and_then(|value| value.to_str().ok()).unwrap_or_default().to_owned();
                        tokens.lock().await.push(token);
                        let value = references.read().await.get(&id).cloned();
                        match value {
                            Some(data) if data["fixtureRouteNotFound"] == true => (StatusCode::NOT_FOUND, Json(json!({
                                "status":404, "reasonCode":"ROUTE_NOT_FOUND",
                                "correlationId":headers.get("x-correlation-id").and_then(|value| value.to_str().ok()).unwrap_or_default()
                            }))).into_response(),
                            Some(data) => (StatusCode::OK, Json(json!({
                                "code":"AVALIACAO_FOUND", "success":true,
                                "correlationId":headers.get("x-correlation-id").and_then(|value| value.to_str().ok()).unwrap_or_default(),
                                "data":data
                            }))).into_response(),
                            None => (StatusCode::NOT_FOUND, Json(json!({
                                "status":404, "reasonCode":"AVALIACAO_NOT_FOUND",
                                "correlationId":headers.get("x-correlation-id").and_then(|value| value.to_str().ok()).unwrap_or_default()
                            }))).into_response(),
                        }
                    }
                })),
        )
        .await
        .unwrap();
    });
    let settings = SecuritySettings::new(
        format!("http://{address}/jwks"),
        "test-issuer",
        "test-audience",
    )
    .unwrap();
    let security = SecurityConfig::initialize(settings).await.unwrap();
    let state = ApplicationState::from_database(
        database,
        Arc::new(AtomicBool::new(true)),
        5,
        AvaliacaoPacienteSettings::new(format!("http://{address}")).unwrap(),
    )
    .unwrap();
    let router = create_router(state, &security);
    let mut header = Header::new(Algorithm::RS256);
    header.kid = Some("test-key".into());
    let pem = private_key.to_pkcs8_pem(LineEnding::LF).unwrap();
    let key = EncodingKey::from_rsa_pem(pem.as_bytes()).unwrap();
    let actor = Uuid::new_v4();
    let token = |subject: Uuid, roles: Vec<&str>| {
        jsonwebtoken::encode(
            &header,
            &json!({
                "sub": subject.to_string(), "iss": "test-issuer", "aud": "test-audience",
                "exp": chrono::Utc::now().timestamp() + 600, "realm_access": {"roles": roles}
            }),
            &key,
        )
        .unwrap()
    };
    (
        router,
        token(actor, vec!["ADMINISTRADOR"]),
        token(Uuid::new_v4(), vec![]),
        token(Uuid::new_v4(), vec!["ADMINISTRADOR"]),
        actor,
        task,
        references,
        observed_tokens,
    )
}

fn request(
    method: &str,
    path: &str,
    token: Option<&str>,
    correlation: Uuid,
    payload: Value,
) -> Request<Body> {
    let mut builder = Request::builder()
        .method(method)
        .uri(path)
        .header("content-type", "application/json")
        .header("x-correlation-id", correlation.to_string());
    if let Some(token) = token {
        builder = builder.header("authorization", format!("Bearer {token}"));
    }
    if method != "GET" {
        builder = builder.header("idempotency-key", Uuid::new_v4().to_string());
    }
    builder.body(Body::from(payload.to_string())).unwrap()
}

async fn json_body(response: Response<Body>) -> Value {
    let bytes = axum::body::to_bytes(response.into_body(), 1024 * 1024)
        .await
        .unwrap();
    serde_json::from_slice(&bytes).unwrap()
}

struct MutationCase<'a> {
    action: &'a str,
    method: &'a str,
    path: &'a str,
    payload: Value,
    conflicting_path: &'a str,
    conflicting_payload: Value,
    invalid_path: &'a str,
    invalid_payload: Value,
    status: StatusCode,
}

fn operation_request(
    case: &MutationCase<'_>,
    token: &str,
    operation: Uuid,
    correlation: Uuid,
    path: &str,
    payload: Value,
) -> Request<Body> {
    let mut value = request(case.method, path, Some(token), correlation, payload);
    value
        .headers_mut()
        .insert("idempotency-key", operation.to_string().parse().unwrap());
    value
}

async fn check_mutation(
    router: &Router,
    database: &DatabaseConfig,
    token: &str,
    other_token: &str,
    case: MutationCase<'_>,
) -> Value {
    let operation = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let left = operation_request(
        &case,
        token,
        operation,
        correlation,
        case.path,
        case.payload.clone(),
    );
    let right = operation_request(
        &case,
        token,
        operation,
        Uuid::new_v4(),
        case.path,
        case.payload.clone(),
    );
    let (left, right) = tokio::join!(router.clone().oneshot(left), router.clone().oneshot(right));
    let mut successful = Vec::new();
    for response in [left.unwrap(), right.unwrap()] {
        let status = response.status();
        let body = json_body(response).await;
        if status == case.status {
            successful.push(body);
        } else {
            assert_eq!(status, StatusCode::CONFLICT, "{}", case.action);
            assert_eq!(
                body["reasonCode"], "OPERATION_IN_PROGRESS",
                "{}",
                case.action
            );
        }
    }
    assert!(!successful.is_empty(), "{}", case.action);
    let response = router
        .clone()
        .oneshot(operation_request(
            &case,
            token,
            operation,
            Uuid::new_v4(),
            case.path,
            case.payload.clone(),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), case.status, "{}", case.action);
    let replayed = json_body(response).await;
    for previous in &successful {
        assert_eq!(previous["data"], replayed["data"], "{}", case.action);
    }
    let status_path = format!(
        "/api/v1/acolhimento/operacoes/{operation}?acao={}",
        case.action
    );
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(token),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK, "{}", case.action);
    assert_eq!(json_body(response).await["data"]["status"], "COMPLETED");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(other_token),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND, "{}", case.action);
    let response = router
        .clone()
        .oneshot(operation_request(
            &case,
            token,
            operation,
            correlation,
            case.conflicting_path,
            case.conflicting_payload.clone(),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::CONFLICT, "{}", case.action);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "OPERATION_CONFLICT"
    );
    let mut unauthorized = request(
        case.method,
        case.path,
        None,
        correlation,
        case.payload.clone(),
    );
    unauthorized
        .headers_mut()
        .insert("idempotency-key", operation.to_string().parse().unwrap());
    let response = router.clone().oneshot(unauthorized).await.unwrap();
    assert_eq!(
        response.status(),
        StatusCode::UNAUTHORIZED,
        "{}",
        case.action
    );
    let client = database.acquire().await.unwrap();
    let changed = client
        .execute(
            "UPDATE acolhimento_operations SET response_status = NULL, response_body = NULL WHERE operation_id = $1",
            &[&operation],
        )
        .await
        .unwrap();
    assert_eq!(changed, 1, "{}", case.action);
    drop(client);
    let response = router
        .clone()
        .oneshot(operation_request(
            &case,
            token,
            operation,
            correlation,
            case.path,
            case.payload.clone(),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::CONFLICT, "{}", case.action);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "OPERATION_COMPLETED"
    );
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(token),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK, "{}", case.action);
    assert_eq!(json_body(response).await["data"]["status"], "COMPLETED");
    let failed_operation = Uuid::new_v4();
    let failed_request = || {
        operation_request(
            &case,
            token,
            failed_operation,
            correlation,
            case.invalid_path,
            case.invalid_payload.clone(),
        )
    };
    let response = router.clone().oneshot(failed_request()).await.unwrap();
    assert_eq!(
        response.status(),
        StatusCode::BAD_REQUEST,
        "{}",
        case.action
    );
    let reason = json_body(response).await["reasonCode"].clone();
    let response = router.clone().oneshot(failed_request()).await.unwrap();
    assert_eq!(
        response.status(),
        StatusCode::BAD_REQUEST,
        "{}",
        case.action
    );
    assert_eq!(
        json_body(response).await["reasonCode"],
        reason,
        "{}",
        case.action
    );
    let failed_status_path = format!(
        "/api/v1/acolhimento/operacoes/{failed_operation}?acao={}",
        case.action
    );
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &failed_status_path,
            Some(token),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK, "{}", case.action);
    assert_eq!(json_body(response).await["data"]["status"], "FAILED");
    replayed
}

