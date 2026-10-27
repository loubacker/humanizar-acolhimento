use std::sync::Arc;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoAvaliacaoRegisterDto, AcolhimentoAvaliacaoRetrieveDto,
};
use crate::application::http::mapper::AcolhimentoAvaliacaoMapper;
use crate::application::validator::AcolhimentoValidator;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::AcolhimentoAvaliacao;
use crate::domain::model::enums::ReasonCode;
use crate::domain::port::AcolhimentoAvaliacaoPort;

pub struct AcolhimentoAvaliacaoService {
    avaliacao_port: Arc<dyn AcolhimentoAvaliacaoPort>,
}

impl AcolhimentoAvaliacaoService {
    pub fn new(avaliacao_port: Arc<dyn AcolhimentoAvaliacaoPort>) -> Self {
        Self { avaliacao_port }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<AcolhimentoAvaliacaoRetrieveDto>, AcolhimentoException> {
        let items = self.avaliacao_port.find_by_patient_id(patient_id).await?;
        Ok(items
            .iter()
            .map(AcolhimentoAvaliacaoMapper::to_dto)
            .collect())
    }

    pub async fn create(
        &self,
        dto: AcolhimentoAvaliacaoRegisterDto,
    ) -> Result<AcolhimentoAvaliacaoRetrieveDto, AcolhimentoException> {
        let (patient_id, avaliacao_paciente_id, titulo) =
            AcolhimentoValidator::validate_avaliacao_register(&dto)?;

        let current = self.avaliacao_port.find_by_patient_id(patient_id).await?;
        if current
            .iter()
            .any(|a| a.avaliacao_paciente_id() == avaliacao_paciente_id)
        {
            return Err(AcolhimentoException::new(
                ReasonCode::AcolhimentoAvaliacaoAlreadyExists,
            ));
        }

        let model = AcolhimentoAvaliacao::create(
            patient_id,
            avaliacao_paciente_id,
            titulo,
            dto.modalidade,
            dto.status,
        );

        let saved = self.avaliacao_port.create(model).await?;
        Ok(AcolhimentoAvaliacaoMapper::to_dto(&saved))
    }

    pub async fn delete_by_id(&self, id: Uuid) -> Result<(), AcolhimentoException> {
        self.avaliacao_port.delete_by_id(id).await
    }
}
