use crate::application::http::dto::{
    AvaliacaoPacienteReferenceDto, AvaliacaoPacienteReferencePageDto,
};
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use uuid::Uuid;

pub(crate) struct AvaliacaoPacienteReferenceValidator;

impl AvaliacaoPacienteReferenceValidator {
    pub fn validate_reference(
        dto: &AvaliacaoPacienteReferenceDto,
        patient_id: Uuid,
        id: Option<Uuid>,
    ) -> Result<(), AcolhimentoException> {
        if dto.id.is_nil()
            || dto.patient_id.is_nil()
            || dto.patient_id != patient_id
            || id.is_some_and(|id| dto.id != id)
            || dto
                .titulo_avaliacao
                .as_ref()
                .is_some_and(|value| value.chars().count() > 255)
            || dto.modalidade.trim().is_empty()
            || dto.modalidade.chars().count() > 50
            || dto.status.trim().is_empty()
            || dto.status.chars().count() > 50
        {
            return Err(invalid());
        }
        Ok(())
    }

    pub fn validate_page(
        dto: &AvaliacaoPacienteReferencePageDto,
        requested_page: u32,
        requested_size: u32,
        expected_total: Option<u64>,
    ) -> Result<(), AcolhimentoException> {
        let offset = u64::from(requested_page) * u64::from(requested_size);
        let remaining = dto.total.checked_sub(offset).ok_or_else(invalid)?;
        let count = u64::try_from(dto.items.len()).map_err(|_| invalid())?;
        if dto.page != requested_page
            || dto.size != requested_size
            || expected_total.is_some_and(|total| total != dto.total)
            || count != remaining.min(u64::from(requested_size))
        {
            return Err(invalid());
        }
        Ok(())
    }
}

fn invalid() -> AcolhimentoException {
    AcolhimentoException::new(ReasonCode::IntegrationFailure)
}
