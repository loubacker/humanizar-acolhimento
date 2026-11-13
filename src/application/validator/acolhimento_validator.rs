use std::collections::HashSet;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoAvaliacaoRegisterDto, AcolhimentoCreateDto, AcolhimentoUpdateDto, NucleoPatientDto,
};
use crate::application::validator::{required, required_uuid};
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;

pub struct AcolhimentoValidator;

impl AcolhimentoValidator {
    pub fn validate_create(dto: &AcolhimentoCreateDto) -> Result<Uuid, AcolhimentoException> {
        let patient_id = required_uuid(dto.patient_id, ReasonCode::PatientIdRequired)?;
        Self::validate_nucleos(&dto.nucleo_patient)?;
        Ok(patient_id)
    }

    pub fn validate_update(dto: &AcolhimentoUpdateDto) -> Result<(), AcolhimentoException> {
        let mut expected = HashSet::new();
        if dto
            .expected_nucleo_patient_ids
            .iter()
            .any(|id| id.is_nil() || !expected.insert(*id))
        {
            return Err(AcolhimentoException::new(
                ReasonCode::RequestParameterInvalid,
            ));
        }
        if dto.nucleo_patient.is_empty() {
            return Ok(());
        }
        Self::validate_nucleos(&dto.nucleo_patient)
    }

    pub fn validate_nucleos(nucleos: &[NucleoPatientDto]) -> Result<(), AcolhimentoException> {
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::NucleoPatientEmpty));
        }

        let mut seen_nucleos = HashSet::new();
        for n in nucleos {
            let nucleo_id = required_uuid(n.nucleo_id, ReasonCode::RequestParameterInvalid)?;
            if !seen_nucleos.insert(nucleo_id) {
                return Err(AcolhimentoException::new(ReasonCode::DuplicateNucleo));
            }
            if n.nucleo_patient_staff.is_empty() {
                return Err(AcolhimentoException::new(ReasonCode::NucleoStaffEmpty));
            }
            let mut seen_staff = HashSet::new();
            for s in &n.nucleo_patient_staff {
                let staff_id = required_uuid(s.staff_user_id, ReasonCode::StaffUserIdRequired)?;
                if !seen_staff.insert(staff_id) {
                    return Err(AcolhimentoException::new(ReasonCode::DuplicateStaff));
                }
                let role = required(s.role.clone(), ReasonCode::RoleRequired)?;
                if role.chars().count() > 100 {
                    return Err(AcolhimentoException::new(ReasonCode::RoleInvalid));
                }
            }
        }

        Ok(())
    }

    pub fn validate_avaliacao_register(
        dto: &AcolhimentoAvaliacaoRegisterDto,
    ) -> Result<(Uuid, Uuid), AcolhimentoException> {
        let patient_id = required_uuid(dto.patient_id, ReasonCode::PatientIdRequired)?;
        let avaliacao_paciente_id = required_uuid(
            dto.avaliacao_paciente_id,
            ReasonCode::AvaliacaoPacienteIdRequired,
        )?;
        Ok((patient_id, avaliacao_paciente_id))
    }
}
