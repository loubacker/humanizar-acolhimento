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

#[tokio::test]
#[ignore = "exige banco descartável acolhimento_isolated_test"]
async fn postgres_http_keeps_patient_data_atomic_and_authorized() {
    let database = database().await;
    let client = database.acquire().await.unwrap();
    let empty: bool = client.query_one(
        "SELECT to_regclass('public.nucleo_patient') IS NULL AND to_regclass('public.dead_letter_quarantine') IS NULL",
        &[],
    ).await.unwrap().get(0);
    assert!(empty, "A fixture exige um banco vazio");
    client
        .batch_execute(include_str!("../migrations/0001_initial_schema.sql"))
        .await
        .unwrap();
    client
        .batch_execute(include_str!(
            "../migrations/0002_dead_letter_quarantine.sql"
        ))
        .await
        .unwrap();
    client
        .batch_execute(include_str!(
            "../migrations/0003_acolhimento_operations.sql"
        ))
        .await
        .unwrap();
    client
        .batch_execute(include_str!(
            "../migrations/0004_avaliacao_titulo_nullable.sql"
        ))
        .await
        .unwrap();
    let patient = Uuid::new_v4();
    let other_patient = Uuid::new_v4();
    let nucleus = Uuid::new_v4();
    let other_nucleus = Uuid::new_v4();
    let section = Uuid::new_v4();
    let other_section = Uuid::new_v4();
    let staff = Uuid::new_v4();
    let staff_link = Uuid::new_v4();
    client.execute(
        "INSERT INTO nucleo_patient (id, patient_id, nucleo_id) VALUES ($1, $2, $3), ($4, $5, $6)",
        &[&section, &patient, &nucleus, &other_section, &other_patient, &other_nucleus],
    ).await.unwrap();
    client.execute(
        "INSERT INTO nucleo_patient_staff (id, nucleo_patient_id, staff_user_id, role) VALUES ($1, $2, $3, $4)",
        &[&staff_link, &section, &staff, &"COORDENADOR"],
    ).await.unwrap();
    let upgrade_period = Uuid::new_v4();
    let upgrade_staff = Uuid::new_v4();
    client.execute("INSERT INTO nucleo_patient (id,patient_id,nucleo_id,anamnese,created_at,updated_at) VALUES ($1,$2,$3,$4,'2025-01-02 10:00:00','2026-01-02 11:00:00')",&[&upgrade_period,&Uuid::new_v4(),&Uuid::new_v4(),&json!({"observacoes":"Anterior a migracao"})]).await.unwrap();
    client.execute("INSERT INTO nucleo_patient_staff (id,nucleo_patient_id,staff_user_id,role) VALUES ($1,$2,$3,'COORDENADOR')",&[&upgrade_staff,&upgrade_period,&Uuid::new_v4()]).await.unwrap();
    let before_upgrade: Value = client
        .query_one(
            "SELECT to_jsonb(n) FROM nucleo_patient n WHERE id=$1",
            &[&upgrade_period],
        )
        .await
        .unwrap()
        .get(0);
    let before_staff: Value = client
        .query_one(
            "SELECT to_jsonb(s) FROM nucleo_patient_staff s WHERE id=$1",
            &[&upgrade_staff],
        )
        .await
        .unwrap()
        .get(0);
    client
        .batch_execute(include_str!(
            "../migrations/0005_nucleo_patient_periodos.sql"
        ))
        .await
        .unwrap();
    let after_upgrade: Value = client.query_one("SELECT to_jsonb(n) - 'desvinculado_at' - 'desvinculado_por' - 'desvinculo_operation_id' FROM nucleo_patient n WHERE id=$1 AND desvinculado_at IS NULL AND desvinculado_por IS NULL AND desvinculo_operation_id IS NULL",&[&upgrade_period]).await.unwrap().get(0);
    let after_staff: Value = client
        .query_one(
            "SELECT to_jsonb(s) FROM nucleo_patient_staff s WHERE id=$1",
            &[&upgrade_staff],
        )
        .await
        .unwrap()
        .get(0);
    assert_eq!(before_upgrade, after_upgrade);
    assert_eq!(before_staff, after_staff);
    drop(client);
    let (router, admin, ordinary, other_admin, actor, task, references, observed_tokens) =
        router(database.clone()).await;
    let correlation = Uuid::new_v4();
    let duplicate_nucleus = json!({"patientId":Uuid::new_v4(), "nucleoPatient":[
        {"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":"COORDENADOR"}]},
        {"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":"COORDENADOR"}]}
    ]});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/register",
            Some(&admin),
            correlation,
            duplicate_nucleus,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
    assert_eq!(json_body(response).await["reasonCode"], "DUPLICATE_NUCLEO");
    let duplicate_staff = json!({"patientId":Uuid::new_v4(), "nucleoPatient":[{"nucleoId":nucleus,"nucleoPatientStaff":[
        {"staffUserId":staff,"role":"COORDENADOR"}, {"staffUserId":staff,"role":"ADMINISTRADOR"}
    ]}]});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/register",
            Some(&admin),
            correlation,
            duplicate_staff,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
    assert_eq!(json_body(response).await["reasonCode"], "DUPLICATE_STAFF");
    let invalid_role = json!({"patientId":Uuid::new_v4(), "nucleoPatient":[{"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":"a".repeat(101)}]}]});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/register",
            Some(&admin),
            correlation,
            invalid_role,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
    assert_eq!(json_body(response).await["reasonCode"], "ROLE_INVALID");
    let anamnese_path = format!("/api/v1/acolhimento/anamnese/{patient}/{nucleus}");
    let detail =
        json!({"dataAnamnese":"2026-10-28", "horaAnamnese":"09:30:00", "observacoes":"teste"});
    let bad = json!({"patientId":patient, "secoes":[
        {"nucleoPatientId":section, "anamnese":detail},
        {"nucleoPatientId":other_section, "anamnese":detail}
    ]});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/anamnese/register",
            Some(&admin),
            correlation,
            bad,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    let client = database.acquire().await.unwrap();
    let row = client.query_one(
        "SELECT (SELECT count(*) FROM acolhimento) AS headers, (SELECT count(*) FROM nucleo_patient WHERE anamnese IS NOT NULL) AS sections", &[],
    ).await.unwrap();
    assert_eq!(row.get::<_, i64>("headers"), 0);
    assert_eq!(row.get::<_, i64>("sections"), 1);
    drop(client);
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &anamnese_path,
            None,
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::UNAUTHORIZED);
    assert_eq!(
        response.headers()["x-correlation-id"],
        correlation.to_string()
    );
    assert_eq!(
        json_body(response).await["correlationId"],
        correlation.to_string()
    );
    let mut detail_with_period = detail.clone();
    detail_with_period["nucleoPatientId"] = json!(section);
    let payload = json!({"payload": detail_with_period});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            &anamnese_path,
            Some(&ordinary),
            correlation,
            payload.clone(),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::FORBIDDEN);
    let (first, second) = tokio::join!(
        router.clone().oneshot(request(
            "POST",
            &anamnese_path,
            Some(&admin),
            correlation,
            payload.clone()
        )),
        router.clone().oneshot(request(
            "POST",
            &anamnese_path,
            Some(&admin),
            correlation,
            payload.clone()
        )),
    );
    let statuses = [first.unwrap().status(), second.unwrap().status()];
    assert!(statuses.contains(&StatusCode::CREATED));
    assert!(statuses.contains(&StatusCode::CONFLICT));
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &anamnese_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    assert_eq!(json_body(response).await["data"]["observacoes"], "teste");
    let update_path = format!("/api/v1/acolhimento/update/{patient}");
    let update = json!({"expectedNucleoPatientIds":[section],"nucleoPatient":[{"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":"COORDENADOR"}]}]});
    let response = router
        .clone()
        .oneshot(request(
            "PUT",
            &update_path,
            Some(&admin),
            correlation,
            update,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    let client = database.acquire().await.unwrap();
    let row = client.query_one("SELECT np.id, np.anamnese, staff.id AS staff_id FROM nucleo_patient np JOIN nucleo_patient_staff staff ON staff.nucleo_patient_id = np.id WHERE np.patient_id = $1 AND np.nucleo_id = $2", &[&patient, &nucleus]).await.unwrap();
    assert_eq!(row.get::<_, Uuid>("id"), section);
    assert_eq!(row.get::<_, Uuid>("staff_id"), staff_link);
    assert_eq!(row.get::<_, Value>("anamnese")["observacoes"], "teste");
    drop(client);
    let mut client = database.acquire().await.unwrap();
    let transaction = client.transaction().await.unwrap();
    transaction
        .query_one(
            "SELECT pg_advisory_xact_lock(hashtextextended($1::text, 0))",
            &[&patient.to_string()],
        )
        .await
        .unwrap();
    let changed = |role: &str| json!({"expectedNucleoPatientIds":[section],"nucleoPatient":[{"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":role}]}]});
    let pending_id = Uuid::new_v4();
    let mut pending_request = request(
        "PUT",
        &update_path,
        Some(&admin),
        correlation,
        changed("ADMINISTRADOR"),
    );
    pending_request
        .headers_mut()
        .insert("idempotency-key", pending_id.to_string().parse().unwrap());
    let left = tokio::spawn(router.clone().oneshot(pending_request));
    let right = tokio::spawn(router.clone().oneshot(request(
        "PUT",
        &update_path,
        Some(&admin),
        correlation,
        changed("TERAPEUTA"),
    )));
    let observer = database.acquire().await.unwrap();
    let mut waiting = false;
    for _ in 0..100 {
        let count: i64 = observer.query_one("SELECT count(*) FROM pg_locks WHERE locktype = 'advisory' AND NOT granted AND database = (SELECT oid FROM pg_database WHERE datname = current_database())", &[]).await.unwrap().get(0);
        if count >= 2 {
            waiting = true;
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(50)).await;
    }
    assert!(
        waiting,
        "As atualizacoes concorrentes nao chegaram ao bloqueio"
    );
    let pending_path =
        format!("/api/v1/acolhimento/operacoes/{pending_id}?acao=ACOLHIMENTO_ATUALIZAR");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &pending_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    assert_eq!(json_body(response).await["data"]["status"], "PENDING");
    transaction.commit().await.unwrap();
    drop(observer);
    drop(client);
    let statuses = [
        left.await.unwrap().unwrap().status(),
        right.await.unwrap().unwrap().status(),
    ];
    assert!(statuses.contains(&StatusCode::OK));
    assert!(statuses.contains(&StatusCode::CONFLICT));
    assert_eq!(
        router
            .clone()
            .oneshot(request(
                "DELETE",
                &anamnese_path,
                Some(&admin),
                correlation,
                json!({"payload":{"nucleoPatientId":section}})
            ))
            .await
            .unwrap()
            .status(),
        StatusCode::OK
    );
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &anamnese_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "ANAMNESE_NOT_FOUND"
    );
    let absent_assessment = Uuid::new_v4();
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/avaliacoes/register",
            Some(&admin),
            correlation,
            json!({"patientId":patient,"avaliacaoPacienteId":absent_assessment}),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "AVALIACAO_PACIENTE_NOT_FOUND"
    );
    let unavailable_assessment = Uuid::new_v4();
    references
        .write()
        .await
        .insert(unavailable_assessment, json!({"fixtureRouteNotFound":true}));
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/avaliacoes/register",
            Some(&admin),
            correlation,
            json!({"patientId":patient,"avaliacaoPacienteId":unavailable_assessment}),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::BAD_GATEWAY);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "INTEGRATION_FAILURE"
    );
    let unavailable_patient = Uuid::new_v4();
    references.write().await.insert(
        Uuid::new_v4(),
        json!({"patientId":unavailable_patient,"fixtureListNotFound":true}),
    );
    let unavailable_list =
        format!("/api/v1/acolhimento/avaliacoes-disponiveis/{unavailable_patient}");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &unavailable_list,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::BAD_GATEWAY);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "INTEGRATION_FAILURE"
    );
    let assessment_id = Uuid::new_v4();
    references.write().await.insert(
        assessment_id,
        json!({
            "id":assessment_id, "patientId":other_patient, "tituloAvaliacao":"Título canônico",
            "modalidade":"RELATORIO_TECNICO", "status":"PENDENTE"
        }),
    );
    let assessment = json!({"patientId":patient, "avaliacaoPacienteId":assessment_id,
        "tituloAvaliacao":"Título adulterado", "status":"ADULTERADO"});
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/avaliacoes/register",
            Some(&admin),
            correlation,
            assessment.clone(),
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::CONFLICT);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "AVALIACAO_PATIENT_MISMATCH"
    );
    references.write().await.insert(
        assessment_id,
        json!({
            "id":assessment_id, "patientId":patient, "tituloAvaliacao":null,
            "modalidade":"RELATORIO_TECNICO", "status":"PENDENTE"
        }),
    );
    let available_path = format!("/api/v1/acolhimento/avaliacoes-disponiveis/{patient}");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &available_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    let available = json_body(response).await;
    assert_eq!(available["data"].as_array().unwrap().len(), 1);
    assert_eq!(available["data"][0]["id"], assessment_id.to_string());
    let response = router
        .clone()
        .oneshot(request(
            "POST",
            "/api/v1/acolhimento/avaliacoes/register",
            Some(&admin),
            correlation,
            assessment,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::CREATED);
    let created_assessment = json_body(response).await;
    assert!(created_assessment["data"]["tituloAvaliacao"].is_null());
    assert_eq!(created_assessment["data"]["status"], "PENDENTE");
    let link_id = created_assessment["data"]["id"].as_str().unwrap();
    references.write().await.insert(
        assessment_id,
        json!({
            "id":assessment_id, "patientId":patient, "tituloAvaliacao":"Título atualizado",
            "modalidade":"RELATORIO_TECNICO", "status":"CONCLUIDO"
        }),
    );
    let list_path = format!("/api/v1/acolhimento/avaliacoes/{patient}");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &list_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    let listed = json_body(response).await;
    assert_eq!(listed["data"][0]["tituloAvaliacao"], "Título atualizado");
    assert_eq!(listed["data"][0]["status"], "CONCLUIDO");
    let delete_path = format!("/api/v1/acolhimento/avaliacoes/{link_id}");
    let response = router
        .clone()
        .oneshot(request(
            "DELETE",
            &delete_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    assert!(references.read().await.contains_key(&assessment_id));
    assert!(
        observed_tokens
            .lock()
            .await
            .iter()
            .all(|token| token == &format!("Bearer {admin}"))
    );
    let new_patient = Uuid::new_v4();
    let create = json!({"patientId":new_patient,"payload":{},"nucleoPatient":[{"nucleoId":Uuid::new_v4(),"nucleoPatientStaff":[{"staffUserId":Uuid::new_v4(),"role":"COORDENADOR"}]}]});
    let operation_id = Uuid::new_v4();
    let mut create_request = request(
        "POST",
        "/api/v1/acolhimento/register",
        Some(&admin),
        correlation,
        create.clone(),
    );
    create_request
        .headers_mut()
        .insert("idempotency-key", operation_id.to_string().parse().unwrap());
    let response = router.clone().oneshot(create_request).await.unwrap();
    assert_eq!(response.status(), StatusCode::CREATED);
    assert_eq!(
        response.headers()["x-correlation-id"],
        correlation.to_string()
    );
    let created = json_body(response).await;
    let replay_correlation = Uuid::new_v4();
    let mut replay = request(
        "POST",
        "/api/v1/acolhimento/register",
        Some(&admin),
        replay_correlation,
        create.clone(),
    );
    replay
        .headers_mut()
        .insert("idempotency-key", operation_id.to_string().parse().unwrap());
    let response = router.clone().oneshot(replay).await.unwrap();
    assert_eq!(response.status(), StatusCode::CREATED);
    let replay_body = json_body(response).await;
    assert_eq!(replay_body["data"], created["data"]);
    assert_eq!(replay_body["correlationId"], replay_correlation.to_string());
    let mut different = create.clone();
    different["patientId"] = json!(Uuid::new_v4());
    let mut conflict = request(
        "POST",
        "/api/v1/acolhimento/register",
        Some(&admin),
        correlation,
        different,
    );
    conflict
        .headers_mut()
        .insert("idempotency-key", operation_id.to_string().parse().unwrap());
    let response = router.clone().oneshot(conflict).await.unwrap();
    assert_eq!(response.status(), StatusCode::CONFLICT);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "OPERATION_CONFLICT"
    );
    let status_path =
        format!("/api/v1/acolhimento/operacoes/{operation_id}?acao=ACOLHIMENTO_CRIAR");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::OK);
    assert_eq!(json_body(response).await["data"]["status"], "COMPLETED");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(&other_admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    let other_action =
        format!("/api/v1/acolhimento/operacoes/{operation_id}?acao=ACOLHIMENTO_ATUALIZAR");
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &other_action,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::NOT_FOUND);
    let client = database.acquire().await.unwrap();
    client
        .execute(
            "UPDATE acolhimento_operations SET response_status = NULL, response_body = NULL WHERE operation_id = $1",
            &[&operation_id],
        )
        .await
        .unwrap();
    drop(client);
    let mut lost_response_replay = request(
        "POST",
        "/api/v1/acolhimento/register",
        Some(&admin),
        correlation,
        create.clone(),
    );
    lost_response_replay
        .headers_mut()
        .insert("idempotency-key", operation_id.to_string().parse().unwrap());
    let response = router.clone().oneshot(lost_response_replay).await.unwrap();
    assert_eq!(response.status(), StatusCode::CONFLICT);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "OPERATION_COMPLETED"
    );
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &status_path,
            Some(&admin),
            correlation,
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(json_body(response).await["data"]["status"], "COMPLETED");
    let mut missing_key = request(
        "POST",
        "/api/v1/acolhimento/register",
        Some(&admin),
        correlation,
        create,
    );
    missing_key.headers_mut().remove("idempotency-key");
    let response = router.clone().oneshot(missing_key).await.unwrap();
    assert_eq!(response.status(), StatusCode::BAD_REQUEST);
    assert_eq!(
        json_body(response).await["reasonCode"],
        "IDEMPOTENCY_KEY_INVALID"
    );
    let client = database.acquire().await.unwrap();
    let row = client
        .query_one(
            "SELECT actor_id, correlation_id FROM outbox_events WHERE correlation_id = $1",
            &[&correlation],
        )
        .await
        .unwrap();
    assert_eq!(row.get::<_, Uuid>("actor_id"), actor);
    assert_eq!(row.get::<_, Uuid>("correlation_id"), correlation);
    drop(client);

    let patient = Uuid::new_v4();
    let nucleus = Uuid::new_v4();
    let staff = Uuid::new_v4();
    let other_id = Uuid::new_v4();
    let register_path = "/api/v1/acolhimento/register";
    let create = json!({"patientId":patient,"nucleoPatient":[{"nucleoId":nucleus,
        "nucleoPatientStaff":[{"staffUserId":staff,"role":"COORDENADOR"}]}]});
    let mut changed_create = create.clone();
    changed_create["patientId"] = json!(other_id);
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ACOLHIMENTO_CRIAR",
            method: "POST",
            path: register_path,
            payload: create,
            conflicting_path: register_path,
            conflicting_payload: changed_create,
            invalid_path: register_path,
            invalid_payload: json!({}),
            status: StatusCode::CREATED,
        },
    )
    .await;

    let update_path = format!("/api/v1/acolhimento/update/{patient}");
    let update_conflict = format!("/api/v1/acolhimento/update/{other_id}");
    let update_invalid = "/api/v1/acolhimento/update/not-a-uuid";
    let period_client = database.acquire().await.unwrap();
    let period_id: Uuid = period_client.query_one("SELECT id FROM nucleo_patient WHERE patient_id=$1 AND nucleo_id=$2 AND desvinculado_at IS NULL",&[&patient,&nucleus]).await.unwrap().get(0);
    drop(period_client);
    let update = json!({"expectedNucleoPatientIds":[period_id],"nucleoPatient":[{"nucleoId":nucleus,
        "nucleoPatientStaff":[{"staffUserId":staff,"role":"ADMINISTRADOR"}]}]});
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ACOLHIMENTO_ATUALIZAR",
            method: "PUT",
            path: &update_path,
            payload: update.clone(),
            conflicting_path: &update_conflict,
            conflicting_payload: update.clone(),
            invalid_path: update_invalid,
            invalid_payload: update,
            status: StatusCode::OK,
        },
    )
    .await;

    let anamnese_path = format!("/api/v1/acolhimento/anamnese/{patient}/{nucleus}");
    let anamnese_conflict = format!("/api/v1/acolhimento/anamnese/{patient}/{other_id}");
    let anamnese_invalid = format!("/api/v1/acolhimento/anamnese/not-a-uuid/{nucleus}");
    let detail = json!({"payload":{"nucleoPatientId":period_id,"dataAnamnese":"2026-10-28",
        "horaAnamnese":"09:30:00","observacoes":"primeira"}});
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ANAMNESE_REGISTRAR",
            method: "POST",
            path: &anamnese_path,
            payload: detail.clone(),
            conflicting_path: &anamnese_conflict,
            conflicting_payload: detail.clone(),
            invalid_path: &anamnese_invalid,
            invalid_payload: detail.clone(),
            status: StatusCode::CREATED,
        },
    )
    .await;
    let changed_detail = json!({"payload":{"nucleoPatientId":period_id,"dataAnamnese":"2026-10-28",
        "horaAnamnese":"10:30:00","observacoes":"atualizada"}});
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ANAMNESE_ATUALIZAR",
            method: "PUT",
            path: &anamnese_path,
            payload: changed_detail.clone(),
            conflicting_path: &anamnese_conflict,
            conflicting_payload: changed_detail.clone(),
            invalid_path: &anamnese_invalid,
            invalid_payload: changed_detail,
            status: StatusCode::OK,
        },
    )
    .await;
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ANAMNESE_EXCLUIR",
            method: "DELETE",
            path: &anamnese_path,
            payload: json!({"payload":{"nucleoPatientId":period_id}}),
            conflicting_path: &anamnese_conflict,
            conflicting_payload: json!({"payload":{"nucleoPatientId":period_id}}),
            invalid_path: &anamnese_invalid,
            invalid_payload: json!({"payload":{"nucleoPatientId":period_id}}),
            status: StatusCode::OK,
        },
    )
    .await;

    let assessment = Uuid::new_v4();
    references.write().await.insert(
        assessment,
        json!({
            "id":assessment,"patientId":patient,"tituloAvaliacao":"Teste sintético",
            "modalidade":"RELATORIO_TECNICO","status":"PENDENTE"
        }),
    );
    let link_path = "/api/v1/acolhimento/avaliacoes/register";
    let link = json!({"patientId":patient,"avaliacaoPacienteId":assessment});
    let conflicting_link = json!({"patientId":other_id,"avaliacaoPacienteId":assessment});
    let linked = check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "AVALIACAO_VINCULAR",
            method: "POST",
            path: link_path,
            payload: link,
            conflicting_path: link_path,
            conflicting_payload: conflicting_link,
            invalid_path: link_path,
            invalid_payload: json!({}),
            status: StatusCode::CREATED,
        },
    )
    .await;
    let link_id = linked["data"]["id"].as_str().unwrap();
    let unlink_path = format!("/api/v1/acolhimento/avaliacoes/{link_id}");
    let unlink_conflict = format!("/api/v1/acolhimento/avaliacoes/{other_id}");
    let unlink_invalid = "/api/v1/acolhimento/avaliacoes/not-a-uuid";
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "AVALIACAO_DESVINCULAR",
            method: "DELETE",
            path: &unlink_path,
            payload: Value::Null,
            conflicting_path: &unlink_conflict,
            conflicting_payload: Value::Null,
            invalid_path: unlink_invalid,
            invalid_payload: Value::Null,
            status: StatusCode::OK,
        },
    )
    .await;
    assert!(references.read().await.contains_key(&assessment));

    let delete_path = format!("/api/v1/acolhimento/delete/{patient}");
    let delete_conflict = format!("/api/v1/acolhimento/delete/{other_id}");
    let delete_invalid = "/api/v1/acolhimento/delete/not-a-uuid";
    check_mutation(
        &router,
        &database,
        &admin,
        &other_admin,
        MutationCase {
            action: "ACOLHIMENTO_EXCLUIR",
            method: "DELETE",
            path: &delete_path,
            payload: Value::Null,
            conflicting_path: &delete_conflict,
            conflicting_payload: Value::Null,
            invalid_path: delete_invalid,
            invalid_payload: Value::Null,
            status: StatusCode::OK,
        },
    )
    .await;
    let client = database.acquire().await.unwrap();
    let row = client.query_one(
        "SELECT (SELECT count(*) FROM nucleo_patient WHERE patient_id = $1 AND desvinculado_at IS NULL) AS links, \
         (SELECT count(*) FROM outbox_events WHERE payload::text LIKE '%' || $1::text || '%') AS events",
        &[&patient],
    ).await.unwrap();
    assert_eq!(row.get::<_, i64>("links"), 0);
    assert_eq!(row.get::<_, i64>("events"), 2);
    verify_periods(&router, &database, &admin, &ordinary, &references, actor).await;
    task.abort();
}

#[tokio::test]
#[ignore = "exige PostgreSQL e vhost RabbitMQ descartáveis"]
async fn rabbit_retains_dlq_until_quarantine_and_deduplicates_callbacks() {
    use lapin::options::{BasicGetOptions, BasicPublishOptions, QueueDeclareOptions};
    use lapin::types::FieldTable;
    use lapin::{BasicProperties, Connection, ConnectionProperties};

    let attempts = Arc::new(std::sync::atomic::AtomicUsize::new(0));
    tracing::subscriber::set_global_default(
        tracing_subscriber::registry().with(QuarantineRetryLayer(attempts.clone())),
    )
    .unwrap();

    let rabbit_url =
        std::env::var("ACOLHIMENTO_TEST_RABBIT_URL").expect("RabbitMQ de teste ausente");
    let parsed = url::Url::parse(&rabbit_url).unwrap();
    assert_eq!(parsed.host_str(), Some("127.0.0.1"));
    assert_eq!(parsed.path(), "/codex-acolhimento-test-1028");
    let database = database().await;
    let client = database.acquire().await.unwrap();
    client
        .batch_execute(
            "CREATE TABLE test_quarantine_failure (payload BYTEA PRIMARY KEY); \
         CREATE FUNCTION test_reject_quarantine() RETURNS trigger LANGUAGE plpgsql AS $$ \
         BEGIN IF EXISTS (SELECT 1 FROM test_quarantine_failure WHERE payload = NEW.payload) \
         THEN RAISE EXCEPTION 'synthetic quarantine failure'; END IF; RETURN NEW; END $$; \
         CREATE TRIGGER test_quarantine_failure BEFORE INSERT ON dead_letter_quarantine \
         FOR EACH ROW EXECUTE FUNCTION test_reject_quarantine();",
        )
        .await
        .unwrap();
    let patient: Uuid = client
        .query_one(
            "SELECT patient_id FROM nucleo_patient ORDER BY id LIMIT 1",
            &[],
        )
        .await
        .unwrap()
        .get(0);
    let mut payload = vec![0, 255, 71, 31];
    payload.extend_from_slice(Uuid::new_v4().as_bytes());
    client
        .execute(
            "INSERT INTO test_quarantine_failure (payload) VALUES ($1)",
            &[&payload],
        )
        .await
        .unwrap();
    drop(client);
    let runtime = RabbitRuntime::start(
        database.clone(),
        RabbitSettings::new(rabbit_url.clone()).unwrap(),
    )
    .await
    .unwrap();
    let connection = Connection::connect(&rabbit_url, ConnectionProperties::default())
        .await
        .unwrap();
    let channel = connection.create_channel().await.unwrap();
    channel
        .basic_publish(
            "".into(),
            QueueCatalog::PATIENT_RETRIEVE_DLQ.into(),
            BasicPublishOptions::default(),
            &payload,
            BasicProperties::default(),
        )
        .await
        .unwrap()
        .await
        .unwrap();
    let mut in_flight = false;
    for _ in 0..100 {
        let queue = channel
            .queue_declare(
                QueueCatalog::PATIENT_RETRIEVE_DLQ.into(),
                QueueDeclareOptions {
                    passive: true,
                    ..Default::default()
                },
                FieldTable::default(),
            )
            .await
            .unwrap();
        if queue.message_count() == 0 {
            in_flight = true;
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
    assert!(in_flight, "Entrega nao foi recebida pelo consumidor");
    for _ in 0..100 {
        if attempts.load(std::sync::atomic::Ordering::SeqCst) > 0 {
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
    assert!(
        attempts.load(std::sync::atomic::Ordering::SeqCst) > 0,
        "A falha de persistencia nao foi observada"
    );
    let client = database.acquire().await.unwrap();
    let count: i64 = client
        .query_one(
            "SELECT count(*) FROM dead_letter_quarantine WHERE payload = $1",
            &[&payload],
        )
        .await
        .unwrap()
        .get(0);
    assert_eq!(count, 0);
    client
        .execute(
            "DELETE FROM test_quarantine_failure WHERE payload = $1",
            &[&payload],
        )
        .await
        .unwrap();
    drop(client);
    let mut persisted = false;
    for _ in 0..150 {
        let client = database.acquire().await.unwrap();
        let row = client
            .query_opt(
                "SELECT payload, properties FROM dead_letter_quarantine WHERE payload = $1",
                &[&payload],
            )
            .await
            .unwrap();
        if let Some(row) = row {
            assert_eq!(row.get::<_, Vec<u8>>("payload"), payload);
            assert!(!row.get::<_, Vec<u8>>("properties").is_empty());
            persisted = true;
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
    assert!(persisted, "Entrega nao persistida apos recuperar o banco");

    let event_id = Uuid::new_v4();
    let correlation = Uuid::new_v4();
    let mut command = json!({
        "eventId":event_id, "correlationId":correlation, "producerService":"synthetic-test",
        "exchangeName":"humanizar.acolhimento.command", "routingKey":"cmd.acolhimento.patient.retrieve.v1",
        "aggregateType":"Patient", "aggregateId":patient, "eventVersion":1,
        "occurredAt":"2026-10-28T10:00:00",
        "payload":{"patientId":patient, "callbackExchange":"humanizar.acolhimento.event"}
    });
    let original = serde_json::to_vec(&command).unwrap();
    for _ in 0..2 {
        channel
            .basic_publish(
                "humanizar.acolhimento.command".into(),
                "cmd.acolhimento.patient.retrieve.v1".into(),
                BasicPublishOptions::default(),
                &original,
                BasicProperties::default(),
            )
            .await
            .unwrap()
            .await
            .unwrap();
    }
    let mut processed = false;
    for _ in 0..100 {
        let client = database.acquire().await.unwrap();
        let count: i64 = client
            .query_one(
                "SELECT count(*) FROM processed_events WHERE event_id = $1",
                &[&event_id],
            )
            .await
            .unwrap()
            .get(0);
        if count == 1 {
            processed = true;
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
    assert!(processed, "Comando nao processado");
    command["payload"]["patientId"] = json!(Uuid::new_v4());
    let divergent = serde_json::to_vec(&command).unwrap();
    channel
        .basic_publish(
            "humanizar.acolhimento.command".into(),
            "cmd.acolhimento.patient.retrieve.v1".into(),
            BasicPublishOptions::default(),
            &divergent,
            BasicProperties::default(),
        )
        .await
        .unwrap()
        .await
        .unwrap();
    let mut quarantined = false;
    for _ in 0..100 {
        let client = database.acquire().await.unwrap();
        let row = client
            .query_one(
                "SELECT EXISTS(SELECT 1 FROM dead_letter_quarantine WHERE payload = $1)",
                &[&divergent],
            )
            .await
            .unwrap();
        if row.get::<_, bool>(0) {
            quarantined = true;
            break;
        }
        tokio::time::sleep(std::time::Duration::from_millis(100)).await;
    }
    assert!(quarantined, "Evento divergente nao chegou a quarentena");
    let client = database.acquire().await.unwrap();
    let row = client.query_one("SELECT (SELECT count(*) FROM processed_events WHERE event_id = $1) AS processed, (SELECT count(*) FROM outbox_events WHERE correlation_id = $2) AS callbacks", &[&event_id, &correlation]).await.unwrap();
    assert_eq!(row.get::<_, i64>("processed"), 1);
    assert_eq!(row.get::<_, i64>("callbacks"), 1);
    drop(client);
    runtime.shutdown().await.unwrap();
    assert!(
        channel
            .basic_get(
                QueueCatalog::PATIENT_RETRIEVE_DLQ.into(),
                BasicGetOptions::default()
            )
            .await
            .unwrap()
            .is_none()
    );
    connection
        .close(200, "teste concluido".into())
        .await
        .unwrap();
    let client = database.acquire().await.unwrap();
    client.batch_execute("DROP TRIGGER test_quarantine_failure ON dead_letter_quarantine; DROP FUNCTION test_reject_quarantine(); DROP TABLE test_quarantine_failure;").await.unwrap();
}

async fn period_request(
    router: &Router,
    method: &str,
    path: &str,
    token: &str,
    body: Value,
    operation: Uuid,
) -> (StatusCode, Value) {
    let mut value = request(method, path, Some(token), Uuid::new_v4(), body);
    if method != "GET" {
        value
            .headers_mut()
            .insert("idempotency-key", operation.to_string().parse().unwrap());
    }
    let response = router.clone().oneshot(value).await.unwrap();
    (response.status(), json_body(response).await)
}

async fn verify_periods(
    router: &Router,
    database: &DatabaseConfig,
    admin: &str,
    ordinary: &str,
    references: &Arc<tokio::sync::RwLock<HashMap<Uuid, Value>>>,
    actor: Uuid,
) {
    let patient = Uuid::new_v4();
    let nucleus = Uuid::new_v4();
    let second_nucleus = Uuid::new_v4();
    let staff = Uuid::new_v4();
    let staff_other = Uuid::new_v4();
    let nuclei = json!([
        {"nucleoId":nucleus,"nucleoPatientStaff":[{"staffUserId":staff,"role":"COORDENADOR"}]},
        {"nucleoId":second_nucleus,"nucleoPatientStaff":[{"staffUserId":staff_other,"role":"COORDENADOR"}]}
    ]);
    assert_eq!(
        period_request(
            router,
            "POST",
            "/api/v1/acolhimento/register",
            admin,
            json!({"patientId":patient,"nucleoPatient":[]}),
            Uuid::new_v4()
        )
        .await
        .0,
        StatusCode::BAD_REQUEST
    );
    let create = json!({"patientId":patient,"nucleoPatient":nuclei});
    let (status, created) = period_request(
        router,
        "POST",
        "/api/v1/acolhimento/register",
        admin,
        create.clone(),
        Uuid::new_v4(),
    )
    .await;
    assert_eq!(status, StatusCode::CREATED);
    let empty_history = format!("/api/v1/acolhimento/paciente/{patient}/historico");
    let (history_status, empty_page) = period_request(
        router,
        "GET",
        &empty_history,
        admin,
        Value::Null,
        Uuid::new_v4(),
    )
    .await;
    assert_eq!(history_status, StatusCode::OK);
    assert_eq!(empty_page["data"]["totalElements"], 0);
    assert!(empty_page["data"]["content"].as_array().unwrap().is_empty());
    let response = router
        .clone()
        .oneshot(request(
            "GET",
            &empty_history,
            None,
            Uuid::new_v4(),
            Value::Null,
        ))
        .await
        .unwrap();
    assert_eq!(response.status(), StatusCode::UNAUTHORIZED);
    let client = database.acquire().await.unwrap();
    let duplicate = client
        .execute(
            "INSERT INTO nucleo_patient (id,patient_id,nucleo_id) VALUES ($1,$2,$3)",
            &[&Uuid::new_v4(), &patient, &nucleus],
        )
        .await
        .unwrap_err();
    assert_eq!(duplicate.code().unwrap().code(), "23505");
    let invalid_close = client.execute("UPDATE nucleo_patient SET desvinculado_at=CURRENT_TIMESTAMP WHERE patient_id=$1 AND nucleo_id=$2",&[&patient,&nucleus]).await.unwrap_err();
    assert_eq!(invalid_close.code().unwrap().code(), "23514");
    drop(client);
}
