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
        Self::validate_nucleos(&dto.nucleo_patient)
    }

    pub fn validate_nucleos(nucleos: &[NucleoPatientDto]) -> Result<(), AcolhimentoException> {
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::NucleoPatientEmpty));
        }

        for n in nucleos {
            required_uuid(n.nucleo_id, ReasonCode::RequestParameterInvalid)?;
            if n.nucleo_patient_staff.is_empty() {
                return Err(AcolhimentoException::new(ReasonCode::NucleoStaffEmpty));
            }
            for s in &n.nucleo_patient_staff {
                required_uuid(s.staff_user_id, ReasonCode::StaffUserIdRequired)?;
                required(s.role.clone(), ReasonCode::RoleRequired)?;
            }
        }

        Ok(())
    }

    pub fn validate_avaliacao_register(
        dto: &AcolhimentoAvaliacaoRegisterDto,
    ) -> Result<(Uuid, Uuid, String), AcolhimentoException> {
        let patient_id = required_uuid(dto.patient_id, ReasonCode::PatientIdRequired)?;
        let avaliacao_paciente_id = required_uuid(
            dto.avaliacao_paciente_id,
            ReasonCode::AvaliacaoPacienteIdRequired,
        )?;
        let titulo = required(
            dto.titulo_avaliacao.clone(),
            ReasonCode::TituloAvaliacaoRequired,
        )?;
        Ok((patient_id, avaliacao_paciente_id, titulo))
    }
}
