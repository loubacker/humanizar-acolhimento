use async_trait::async_trait;
use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxEvent;
use crate::domain::model::{NucleoPatient, NucleoPatientStaff};
use crate::domain::port::NucleoPatientPort;
use crate::infrastructure::persistence::entity::{NucleoPatientEntity, NucleoPatientStaffEntity};
use crate::infrastructure::persistence::repository::NucleoPatientRepository;

pub(crate) struct NucleoPatientAdapter {
    repository: NucleoPatientRepository,
}

impl NucleoPatientAdapter {
    pub fn new(repository: NucleoPatientRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl NucleoPatientPort for NucleoPatientAdapter {
    async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<NucleoPatient>, AcolhimentoException> {
        let rows = self.repository.find_by_patient_id(patient_id).await?;
        Ok(rows
            .into_iter()
            .map(|e| {
                NucleoPatient::restore(
                    e.id,
                    e.patient_id,
                    e.nucleo_id,
                    e.anamnese,
                    e.created_at,
                    e.updated_at,
                )
            })
            .collect())
    }

    async fn find_by_id(&self, id: Uuid) -> Result<Option<NucleoPatient>, AcolhimentoException> {
        let opt = self.repository.find_by_id(id).await?;
        Ok(opt.map(|e| {
            NucleoPatient::restore(
                e.id,
                e.patient_id,
                e.nucleo_id,
                e.anamnese,
                e.created_at,
                e.updated_at,
            )
        }))
    }

    async fn replace_patient_nucleos(
        &self,
        patient_id: Uuid,
        entries: Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>, AcolhimentoException> {
        let mapped_entries = entries
            .into_iter()
            .map(|(n, staff_vec)| {
                let n_entity = NucleoPatientEntity {
                    id: n.id(),
                    patient_id: n.patient_id(),
                    nucleo_id: n.nucleo_id(),
                    anamnese: n.anamnese().cloned(),
                    created_at: n.created_at(),
                    updated_at: n.updated_at(),
                };
                let staff_entities = staff_vec
                    .into_iter()
                    .map(|s| NucleoPatientStaffEntity {
                        id: s.id(),
                        nucleo_patient_id: s.nucleo_patient_id(),
                        staff_user_id: s.staff_user_id(),
                        role: s.role().to_owned(),
                    })
                    .collect();
                (n_entity, staff_entities)
            })
            .collect();

        let saved = self
            .repository
            .replace_patient_nucleos(patient_id, mapped_entries, outbox_events)
            .await?;

        Ok(saved
            .into_iter()
            .map(|(n_e, s_vec)| {
                let n = NucleoPatient::restore(
                    n_e.id,
                    n_e.patient_id,
                    n_e.nucleo_id,
                    n_e.anamnese,
                    n_e.created_at,
                    n_e.updated_at,
                );
                let staff = s_vec
                    .into_iter()
                    .map(|s| {
                        NucleoPatientStaff::restore(
                            s.id,
                            s.nucleo_patient_id,
                            s.staff_user_id,
                            s.role,
                        )
                    })
                    .collect();
                (n, staff)
            })
            .collect())
    }

    async fn update_anamnese_batch(
        &self,
        updates: Vec<(Uuid, Option<Value>)>,
    ) -> Result<Vec<NucleoPatient>, AcolhimentoException> {
        let now = chrono::Local::now().naive_local();
        let entity_updates = updates
            .into_iter()
            .map(|(id, val)| (id, val, now))
            .collect();
        let updated = self
            .repository
            .update_anamnese_batch(entity_updates)
            .await?;
        Ok(updated
            .into_iter()
            .map(|e| {
                NucleoPatient::restore(
                    e.id,
                    e.patient_id,
                    e.nucleo_id,
                    e.anamnese,
                    e.created_at,
                    e.updated_at,
                )
            })
            .collect())
    }

    async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<(), AcolhimentoException> {
        self.repository
            .delete_by_patient_id(patient_id, outbox_events)
            .await
    }
}
