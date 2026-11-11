use futures_util::{StreamExt, TryStreamExt, stream};
use std::sync::Arc;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoAvaliacaoRegisterDto, AcolhimentoAvaliacaoRetrieveDto, AvaliacaoDisponivelDto,
};
use crate::application::http::mapper::AcolhimentoAvaliacaoMapper;
use crate::application::validator::AcolhimentoValidator;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{AcolhimentoAvaliacao, DelegatedAccess, OperationIdentity};
use crate::domain::port::{
    AcolhimentoAvaliacaoPort, AvaliacaoPacienteReferencePort, NucleoPatientPort,
};

pub struct AcolhimentoAvaliacaoService {
    avaliacao_port: Arc<dyn AcolhimentoAvaliacaoPort>,
    reference_port: Arc<dyn AvaliacaoPacienteReferencePort>,
    nucleo_patient_port: Arc<dyn NucleoPatientPort>,
}

impl AcolhimentoAvaliacaoService {
    pub fn new(
        avaliacao_port: Arc<dyn AcolhimentoAvaliacaoPort>,
        reference_port: Arc<dyn AvaliacaoPacienteReferencePort>,
        nucleo_patient_port: Arc<dyn NucleoPatientPort>,
    ) -> Self {
        Self {
            avaliacao_port,
            reference_port,
            nucleo_patient_port,
        }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Vec<AcolhimentoAvaliacaoRetrieveDto>, AcolhimentoException> {
        let items = self.avaliacao_port.find_by_patient_id(patient_id).await?;
        stream::iter(items)
            .map(|item| async move {
                let canonical = self
                    .reference_port
                    .find_by_id(item.avaliacao_paciente_id(), access, correlation_id)
                    .await?
                    .ok_or_else(|| {
                        AcolhimentoException::new(ReasonCode::AvaliacaoPacienteNotFound)
                    })?;
                if canonical.id != item.avaliacao_paciente_id() {
                    return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
                }
                if canonical.patient_id != patient_id {
                    return Err(AcolhimentoException::new(
                        ReasonCode::AvaliacaoPatientMismatch,
                    ));
                }
                let mut dto = AcolhimentoAvaliacaoMapper::to_dto(&item);
                dto.titulo_avaliacao = canonical.titulo;
                dto.modalidade = Some(canonical.modalidade);
                dto.status = Some(canonical.status);
                Ok(dto)
            })
            .buffered(4)
            .try_collect()
            .await
    }

    pub async fn find_available(
        &self,
        patient_id: Uuid,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<Vec<AvaliacaoDisponivelDto>, AcolhimentoException> {
        let values = self
            .reference_port
            .find_by_patient_id(patient_id, access, correlation_id)
            .await?;
        Ok(values
            .into_iter()
            .map(AcolhimentoAvaliacaoMapper::to_disponivel)
            .collect())
    }

    pub async fn create(
        &self,
        dto: AcolhimentoAvaliacaoRegisterDto,
        operation: &OperationIdentity,
        access: &DelegatedAccess,
        correlation_id: Uuid,
    ) -> Result<AcolhimentoAvaliacaoRetrieveDto, AcolhimentoException> {
        let (patient_id, avaliacao_paciente_id) =
            AcolhimentoValidator::validate_avaliacao_register(&dto)?;

        if self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?
            .is_empty()
        {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }
        let canonical = self
            .reference_port
            .find_by_id(avaliacao_paciente_id, access, correlation_id)
            .await?
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::AvaliacaoPacienteNotFound))?;
        if canonical.id != avaliacao_paciente_id {
            return Err(AcolhimentoException::new(ReasonCode::IntegrationFailure));
        }
        if canonical.patient_id != patient_id {
            return Err(AcolhimentoException::new(
                ReasonCode::AvaliacaoPatientMismatch,
            ));
        }

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
            canonical.titulo,
            Some(canonical.modalidade),
            Some(canonical.status),
        );

        let saved = self.avaliacao_port.create(model, operation).await?;
        Ok(AcolhimentoAvaliacaoMapper::to_dto(&saved))
    }

    pub async fn delete_by_id(
        &self,
        id: Uuid,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        self.avaliacao_port.delete_by_id(id, operation).await
    }
}
