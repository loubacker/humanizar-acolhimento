#![forbid(unsafe_code)]

use humanizar_acolhimento::application::http::dto::{
    AcolhimentoUpdateDto, AnamneseDeleteRequestDto, AnamneseRequestDto,
};
use humanizar_acolhimento::application::messaging::inbound::mapper::AcolhimentoCommandMapper;
use serde_json::json;
use uuid::Uuid;

#[test]
fn command_hash_distinguishes_replays_from_divergent_content() {
    let event_id = Uuid::new_v4();
    let mut envelope = json!({
        "eventId": event_id,
        "correlationId": Uuid::new_v4(),
        "producerService": "synthetic-test",
        "exchangeName": "humanizar.acolhimento.command",
        "routingKey": "cmd.acolhimento.patient.retrieve.v1",
        "aggregateType": "Patient",
        "aggregateId": Uuid::new_v4(),
        "eventVersion": 1,
        "occurredAt": "2026-10-28T10:00:00",
        "payload": {"patientId": Uuid::new_v4(), "callbackExchange": "humanizar.acolhimento.event"}
    });
    let original = serde_json::to_vec(&envelope).unwrap();
    let parse = |body: &[u8]| {
        AcolhimentoCommandMapper::parse(
            body,
            "humanizar.acolhimento.command",
            "cmd.acolhimento.patient.retrieve.v1",
        )
        .unwrap()
    };
    let first = parse(&original);
    let replay = parse(&original);
    assert_eq!(first.event_id, replay.event_id);
    assert_eq!(first.event_hash, replay.event_hash);
    envelope["payload"]["patientId"] = json!(Uuid::new_v4());
    let changed = parse(&serde_json::to_vec(&envelope).unwrap());
    assert_eq!(first.event_id, changed.event_id);
    assert_ne!(first.event_hash, changed.event_hash);
}

#[test]
fn empty_update_requires_an_explicit_array() {
    let update: AcolhimentoUpdateDto =
        serde_json::from_value(json!({"nucleoPatient":[],"expectedNucleoPatientIds":[]})).unwrap();
    assert!(update.nucleo_patient.is_empty());
    for body in [
        json!({}),
        json!({"nucleoPatient":[]}),
        json!({"nucleoPatient":[],"expectedNucleoPatientIds":null}),
        json!({"nucleoPatient":null,"expectedNucleoPatientIds":[]}),
    ] {
        assert!(serde_json::from_value::<AcolhimentoUpdateDto>(body).is_err());
    }
}

