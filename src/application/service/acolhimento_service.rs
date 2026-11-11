use std::collections::{HashMap, HashSet};
use std::sync::Arc;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoCreateDto, AcolhimentoPatientStatusDto, AcolhimentoRetrieveDto, AcolhimentoUpdateDto,
};
use crate::application::http::mapper::AcolhimentoMapper;
use crate::application::messaging::outbound::dto::{StaffDesvinculadoDto, StaffVinculadoDto};
use crate::application::messaging::outbound::factory::StaffOutboxFactory;
use crate::application::validator::AcolhimentoValidator;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{NucleoPatient, NucleoPatientStaff, OperationIdentity};
use crate::domain::port::{AcolhimentoPort, NucleoPatientPort, NucleoPatientStaffPort};

pub struct AcolhimentoService {
    nucleo_patient_port: Arc<dyn NucleoPatientPort>,
    nucleo_patient_staff_port: Arc<dyn NucleoPatientStaffPort>,
    acolhimento_port: Arc<dyn AcolhimentoPort>,
    max_attempts: u32,
}

impl AcolhimentoService {
    pub fn new(
        nucleo_patient_port: Arc<dyn NucleoPatientPort>,
        nucleo_patient_staff_port: Arc<dyn NucleoPatientStaffPort>,
        acolhimento_port: Arc<dyn AcolhimentoPort>,
        max_attempts: u32,
    ) -> Self {
        Self {
            nucleo_patient_port,
            nucleo_patient_staff_port,
            acolhimento_port,
            max_attempts,
        }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<AcolhimentoRetrieveDto, AcolhimentoException> {
        let nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let acolhimento = self.acolhimento_port.find_by_patient_id(patient_id).await?;
        let nucleo_patient_ids: Vec<Uuid> = nucleos.iter().map(|n| n.id()).collect();
        let staff_list = self
            .nucleo_patient_staff_port
            .find_by_nucleo_patient_ids(&nucleo_patient_ids)
            .await?;

        let mut staff_by_np: HashMap<Uuid, Vec<NucleoPatientStaff>> = HashMap::new();
        for s in staff_list {
            staff_by_np
                .entry(s.nucleo_patient_id())
                .or_default()
                .push(s);
        }

        Ok(AcolhimentoMapper::to_dto(
            patient_id,
            acolhimento.as_ref(),
            &nucleos,
            &staff_by_np,
        ))
    }

    pub async fn find_status_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<AcolhimentoPatientStatusDto, AcolhimentoException> {
        let nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        Ok(AcolhimentoPatientStatusDto {
            patient_id,
            has_acolhimento: !nucleos.is_empty(),
        })
    }

    pub async fn create(
        &self,
        dto: AcolhimentoCreateDto,
        actor_id: Option<Uuid>,
        correlation_id: Uuid,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoRetrieveDto, AcolhimentoException> {
        let patient_id = AcolhimentoValidator::validate_create(&dto)?;
        let existing = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if !existing.is_empty() {
            return Err(AcolhimentoException::new(
                ReasonCode::AcolhimentoAlreadyExists,
            ));
        }

        let now = chrono::Utc::now().naive_utc();
        let mut entries = Vec::new();
        let mut outbox_events = Vec::new();

        for n_dto in dto.nucleo_patient {
            let nucleo_id = n_dto.nucleo_id.unwrap();
            let nucleo_patient = NucleoPatient::create(patient_id, nucleo_id);

            let mut staff_items = Vec::new();
            for s_dto in n_dto.nucleo_patient_staff {
                let staff_user_id = s_dto.staff_user_id.unwrap();
                let role = s_dto.role.unwrap().trim().to_owned();
                let staff_member =
                    NucleoPatientStaff::create(nucleo_patient.id(), staff_user_id, role.clone());
                staff_items.push(staff_member);

                let event = StaffOutboxFactory::create_vinculado(
                    correlation_id,
                    actor_id,
                    user_agent.clone(),
                    origin_ip.clone(),
                    StaffVinculadoDto {
                        nucleo_patient_id: nucleo_patient.id(),
                        patient_id,
                        nucleo_id,
                        staff_user_id,
                        role,
                    },
                    now,
                    self.max_attempts,
                )?;
                outbox_events.push(event);
            }

            entries.push((nucleo_patient, staff_items));
        }

        let saved = self
            .nucleo_patient_port
            .replace_patient_nucleos(patient_id, &[], &[], entries, outbox_events, operation)
            .await?;

        let saved_nucleos: Vec<NucleoPatient> = saved.iter().map(|(n, _)| n.clone()).collect();
        let mut staff_by_np: HashMap<Uuid, Vec<NucleoPatientStaff>> = HashMap::new();
        for (n, staff_vec) in saved {
            staff_by_np.insert(n.id(), staff_vec);
        }

        Ok(AcolhimentoMapper::to_dto(
            patient_id,
            None,
            &saved_nucleos,
            &staff_by_np,
        ))
    }

    pub async fn update_by_patient_id(
        &self,
        patient_id: Uuid,
        dto: AcolhimentoUpdateDto,
        actor_id: Option<Uuid>,
        correlation_id: Uuid,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoRetrieveDto, AcolhimentoException> {
        AcolhimentoValidator::validate_update(&dto)?;

        let old_nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if old_nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let old_np_ids: Vec<Uuid> = old_nucleos.iter().map(|n| n.id()).collect();
        let old_staff_list = self
            .nucleo_patient_staff_port
            .find_by_nucleo_patient_ids(&old_np_ids)
            .await?;

        let mut old_bindings: HashMap<(Uuid, Uuid), Uuid> = HashMap::new();
        for s in &old_staff_list {
            if let Some(n) = old_nucleos.iter().find(|n| n.id() == s.nucleo_patient_id()) {
                old_bindings.insert((n.nucleo_id(), s.staff_user_id()), s.nucleo_patient_id());
            }
        }

        let now = chrono::Utc::now().naive_utc();
        let mut entries = Vec::new();
        let mut outbox_events = Vec::new();
        let mut new_bindings = HashSet::new();

        for n_dto in dto.nucleo_patient {
            let nucleo_id = n_dto.nucleo_id.unwrap();
            let nucleo_patient = old_nucleos
                .iter()
                .find(|existing| existing.nucleo_id() == nucleo_id)
                .cloned()
                .unwrap_or_else(|| NucleoPatient::create(patient_id, nucleo_id));

            let mut staff_items = Vec::new();
            for s_dto in n_dto.nucleo_patient_staff {
                let staff_user_id = s_dto.staff_user_id.unwrap();
                let role = s_dto.role.unwrap().trim().to_owned();
                let staff_member = old_staff_list
                    .iter()
                    .find(|staff| {
                        staff.nucleo_patient_id() == nucleo_patient.id()
                            && staff.staff_user_id() == staff_user_id
                    })
                    .map(|staff| {
                        NucleoPatientStaff::restore(
                            staff.id(),
                            nucleo_patient.id(),
                            staff_user_id,
                            role.clone(),
                        )
                    })
                    .unwrap_or_else(|| {
                        NucleoPatientStaff::create(nucleo_patient.id(), staff_user_id, role.clone())
                    });
                staff_items.push(staff_member);
                new_bindings.insert((nucleo_id, staff_user_id));

                if !old_bindings.contains_key(&(nucleo_id, staff_user_id)) {
                    let event = StaffOutboxFactory::create_vinculado(
                        correlation_id,
                        actor_id,
                        user_agent.clone(),
                        origin_ip.clone(),
                        StaffVinculadoDto {
                            nucleo_patient_id: nucleo_patient.id(),
                            patient_id,
                            nucleo_id,
                            staff_user_id,
                            role,
                        },
                        now,
                        self.max_attempts,
                    )?;
                    outbox_events.push(event);
                }
            }

            entries.push((nucleo_patient, staff_items));
        }

        for ((nucleo_id, staff_user_id), old_np_id) in old_bindings {
            if !new_bindings.contains(&(nucleo_id, staff_user_id)) {
                let event = StaffOutboxFactory::create_desvinculado(
                    correlation_id,
                    actor_id,
                    user_agent.clone(),
                    origin_ip.clone(),
                    StaffDesvinculadoDto {
                        nucleo_patient_id: old_np_id,
                        patient_id,
                        nucleo_id,
                        staff_user_id,
                    },
                    now,
                    self.max_attempts,
                )?;
                outbox_events.push(event);
            }
        }

        let saved = self
            .nucleo_patient_port
            .replace_patient_nucleos(
                patient_id,
                &old_nucleos,
                &old_staff_list,
                entries,
                outbox_events,
                operation,
            )
            .await?;

        let saved_nucleos: Vec<NucleoPatient> = saved.iter().map(|(n, _)| n.clone()).collect();
        let mut staff_by_np: HashMap<Uuid, Vec<NucleoPatientStaff>> = HashMap::new();
        for (n, staff_vec) in saved {
            staff_by_np.insert(n.id(), staff_vec);
        }

        let acolhimento = self.acolhimento_port.find_by_patient_id(patient_id).await?;

        Ok(AcolhimentoMapper::to_dto(
            patient_id,
            acolhimento.as_ref(),
            &saved_nucleos,
            &staff_by_np,
        ))
    }

    pub async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        actor_id: Option<Uuid>,
        correlation_id: Uuid,
        user_agent: Option<String>,
        origin_ip: Option<String>,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let old_nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if old_nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let old_np_ids: Vec<Uuid> = old_nucleos.iter().map(|n| n.id()).collect();
        let old_staff_list = self
            .nucleo_patient_staff_port
            .find_by_nucleo_patient_ids(&old_np_ids)
            .await?;

        let now = chrono::Utc::now().naive_utc();
        let mut outbox_events = Vec::new();

        for s in &old_staff_list {
            if let Some(n) = old_nucleos.iter().find(|n| n.id() == s.nucleo_patient_id()) {
                let event = StaffOutboxFactory::create_desvinculado(
                    correlation_id,
                    actor_id,
                    user_agent.clone(),
                    origin_ip.clone(),
                    StaffDesvinculadoDto {
                        nucleo_patient_id: s.nucleo_patient_id(),
                        patient_id,
                        nucleo_id: n.nucleo_id(),
                        staff_user_id: s.staff_user_id(),
                    },
                    now,
                    self.max_attempts,
                )?;
                outbox_events.push(event);
            }
        }

        self.nucleo_patient_port
            .delete_by_patient_id(
                patient_id,
                &old_nucleos,
                &old_staff_list,
                outbox_events,
                operation,
            )
            .await?;

        Ok(())
    }
}
