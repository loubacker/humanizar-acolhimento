use async_trait::async_trait;
use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::messaging::OutboxEvent;
use crate::domain::model::{
    Acolhimento, AnamneseOperation, NucleoPatient, NucleoPatientHistory, NucleoPatientHistoryPage,
    NucleoPatientStaff, OperationIdentity,
};
use crate::domain::port::NucleoPatientPort;
use crate::infrastructure::persistence::entity::{
    AcolhimentoEntity, NucleoPatientEntity, NucleoPatientHistoryEntity, NucleoPatientStaffEntity,
};
use crate::infrastructure::persistence::repository::{
    NucleoPatientHistoryRepository, NucleoPatientRepository,
};

pub(crate) struct NucleoPatientAdapter {
    repository: NucleoPatientRepository,
    history_repository: NucleoPatientHistoryRepository,
}

impl NucleoPatientAdapter {
    pub fn new(
        repository: NucleoPatientRepository,
        history_repository: NucleoPatientHistoryRepository,
    ) -> Self {
        Self {
            repository,
            history_repository,
        }
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
        expected_nucleos: &[NucleoPatient],
        expected_staff: &[NucleoPatientStaff],
        entries: Vec<(NucleoPatient, Vec<NucleoPatientStaff>)>,
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
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
            .replace_patient_nucleos(
                patient_id,
                expected_nucleos.iter().map(to_nucleo_entity).collect(),
                expected_staff.iter().map(to_staff_entity).collect(),
                mapped_entries,
                outbox_events,
                operation,
            )
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

    async fn history(
        &self,
        patient_id: Uuid,
        page: u32,
        size: u32,
    ) -> Result<NucleoPatientHistoryPage, AcolhimentoException> {
        let (entries, staff, total_elements) =
            self.history_repository.list(patient_id, page, size).await?;
        Ok(NucleoPatientHistoryPage {
            content: map_history(entries, staff),
            page,
            size,
            total_elements,
        })
    }

    async fn history_detail(
        &self,
        patient_id: Uuid,
        id: Uuid,
    ) -> Result<Option<NucleoPatientHistory>, AcolhimentoException> {
        let (entries, staff, _) = self.history_repository.detail(patient_id, id).await?;
        Ok(map_history(entries, staff).into_iter().next())
    }

    async fn save_anamnese(
        &self,
        acolhimento: Acolhimento,
        updates: Vec<(Uuid, Option<Value>)>,
        operation: &OperationIdentity,
    ) -> Result<Acolhimento, AcolhimentoException> {
        let entity = AcolhimentoEntity {
            id: acolhimento.id(),
            patient_id: acolhimento.patient_id(),
            data_anamnese: acolhimento.data_anamnese(),
            hora_anamnese: acolhimento.hora_anamnese(),
            observacoes: acolhimento.observacoes().map(ToOwned::to_owned),
            created_at: acolhimento.created_at(),
            updated_at: acolhimento.updated_at(),
        };
        let saved = self
            .repository
            .save_anamnese(entity, updates, operation)
            .await?;
        Ok(Acolhimento::restore(
            saved.id,
            saved.patient_id,
            saved.data_anamnese,
            saved.hora_anamnese,
            saved.observacoes,
            saved.created_at,
            saved.updated_at,
        ))
    }

    async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        expected_nucleos: &[NucleoPatient],
        expected_staff: &[NucleoPatientStaff],
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        self.repository
            .delete_by_patient_id(
                patient_id,
                expected_nucleos.iter().map(to_nucleo_entity).collect(),
                expected_staff.iter().map(to_staff_entity).collect(),
                outbox_events,
                operation,
            )
            .await
    }

    async fn change_nucleo_anamnese(
        &self,
        patient_id: Uuid,
        nucleo_id: Uuid,
        nucleo_patient_id: Uuid,
        operation: AnamneseOperation,
        anamnese: Option<Value>,
        operation_identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        self.repository
            .change_nucleo_anamnese(
                patient_id,
                nucleo_id,
                nucleo_patient_id,
                operation,
                anamnese,
                operation_identity,
            )
            .await
    }
}

fn to_nucleo_entity(nucleo: &NucleoPatient) -> NucleoPatientEntity {
    NucleoPatientEntity {
        id: nucleo.id(),
        patient_id: nucleo.patient_id(),
        nucleo_id: nucleo.nucleo_id(),
        anamnese: nucleo.anamnese().cloned(),
        created_at: nucleo.created_at(),
        updated_at: nucleo.updated_at(),
    }
}

fn to_staff_entity(staff: &NucleoPatientStaff) -> NucleoPatientStaffEntity {
    NucleoPatientStaffEntity {
        id: staff.id(),
        nucleo_patient_id: staff.nucleo_patient_id(),
        staff_user_id: staff.staff_user_id(),
        role: staff.role().to_owned(),
    }
}

fn map_history(
    entries: Vec<NucleoPatientHistoryEntity>,
    staff: Vec<NucleoPatientStaffEntity>,
) -> Vec<NucleoPatientHistory> {
    let mut grouped = std::collections::HashMap::<Uuid, Vec<NucleoPatientStaff>>::new();
    for staff in staff {
        grouped
            .entry(staff.nucleo_patient_id)
            .or_default()
            .push(NucleoPatientStaff::restore(
                staff.id,
                staff.nucleo_patient_id,
                staff.staff_user_id,
                staff.role,
            ));
    }
    entries
        .into_iter()
        .map(|entry| NucleoPatientHistory {
            id: entry.id,
            nucleo_id: entry.nucleo_id,
            created_at: entry.created_at,
            desvinculado_at: entry.desvinculado_at,
            desvinculado_por: entry.desvinculado_por,
            desvinculo_operation_id: entry.desvinculo_operation_id,
            possui_anamnese: entry.possui_anamnese,
            anamnese: entry.anamnese,
            responsaveis: grouped.remove(&entry.id).unwrap_or_default(),
        })
        .collect()
}
