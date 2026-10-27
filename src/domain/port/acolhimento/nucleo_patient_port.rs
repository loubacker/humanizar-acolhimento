use async_trait::async_trait;
use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxEvent;
use crate::domain::model::{NucleoPatient, NucleoPatientStaff};

#[async_trait]
pub trait NucleoPatientPort: Send + Sync {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<NucleoPatient>, AcolhimentoException>;

    async fn find_by_id(&self, id: Uuid) -> Result<Option<NucleoPatient>, AcolhimentoException>;

    async fn replace_patient_nucleos(
        &self,
        patient_id: Uuid,
        entries: Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>, AcolhimentoException>;

    async fn update_anamnese_batch(
        &self,
        updates: Vec<(Uuid, Option<Value>)>,
    ) -> Result<Vec<NucleoPatient>, AcolhimentoException>;

    async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<(), AcolhimentoException>;
}
