use async_trait::async_trait;
use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxEvent;
use crate::domain::model::{
    Acolhimento, AnamneseOperation, NucleoPatient, NucleoPatientStaff, OperationIdentity,
};

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
        expected_nucleos: &[NucleoPatient],
        expected_staff: &[NucleoPatientStaff],
        entries: Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>,
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
    ) -> Result<Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>, AcolhimentoException>;

    async fn save_anamnese(
        &self,
        acolhimento: Acolhimento,
        updates: Vec<(Uuid, Option<Value>)>,
        operation: &OperationIdentity,
    ) -> Result<Acolhimento, AcolhimentoException>;

    async fn change_nucleo_anamnese(
        &self,
        patient_id: Uuid,
        nucleo_id: Uuid,
        nucleo_patient_id: Uuid,
        operation: AnamneseOperation,
        anamnese: Option<Value>,
        operation_identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException>;

    async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        expected_nucleos: &[NucleoPatient],
        expected_staff: &[NucleoPatientStaff],
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException>;
}
