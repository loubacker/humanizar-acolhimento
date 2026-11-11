use std::sync::Arc;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto, AnamneseDetailDto,
    AnamneseItemDto,
};
use crate::application::http::mapper::AcolhimentoAnamneseMapper;
use crate::application::validator::required_uuid;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{Acolhimento, AnamneseOperation, OperationIdentity};
use crate::domain::port::{AcolhimentoPort, NucleoPatientPort};

pub struct AcolhimentoAnamneseService {
    acolhimento_port: Arc<dyn AcolhimentoPort>,
    nucleo_patient_port: Arc<dyn NucleoPatientPort>,
}

impl AcolhimentoAnamneseService {
    pub fn new(
        acolhimento_port: Arc<dyn AcolhimentoPort>,
        nucleo_patient_port: Arc<dyn NucleoPatientPort>,
    ) -> Self {
        Self {
            acolhimento_port,
            nucleo_patient_port,
        }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<AcolhimentoAnamneseRetrieveDto, AcolhimentoException> {
        let nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let acolhimento = self
            .acolhimento_port
            .find_by_patient_id(patient_id)
            .await?
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::AnamneseNotFound))?;

        Ok(AcolhimentoAnamneseMapper::to_dto(&acolhimento, &nucleos))
    }

    pub async fn find_by_nucleo_patient_id(
        &self,
        nucleo_patient_id: Uuid,
    ) -> Result<AnamneseItemDto, AcolhimentoException> {
        let nucleo = self
            .nucleo_patient_port
            .find_by_id(nucleo_patient_id)
            .await?
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::NucleoPatientNotFound))?;

        Ok(AnamneseItemDto {
            nucleo_patient_id: nucleo.id(),
            nucleo_id: nucleo.nucleo_id(),
            anamnese: nucleo.anamnese().cloned(),
        })
    }

    pub async fn find_by_patient_and_nucleo(
        &self,
        patient_id: Uuid,
        nucleo_id: Uuid,
    ) -> Result<AnamneseDetailDto, AcolhimentoException> {
        let nucleo = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?
            .into_iter()
            .find(|nucleo| nucleo.nucleo_id() == nucleo_id)
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::NucleoPatientNotFound))?;
        let value = nucleo
            .anamnese()
            .cloned()
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::AnamneseNotFound))?;
        serde_json::from_value(value).map_err(|error| {
            AcolhimentoException::with_source(ReasonCode::PersistenceFailure, error)
        })
    }

    pub async fn change_nucleo_anamnese(
        &self,
        patient_id: Uuid,
        nucleo_id: Uuid,
        operation: AnamneseOperation,
        detail: Option<AnamneseDetailDto>,
        operation_identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let anamnese = detail
            .map(serde_json::to_value)
            .transpose()
            .map_err(|error| {
                AcolhimentoException::with_source(ReasonCode::ValidationError, error)
            })?;
        self.nucleo_patient_port
            .change_nucleo_anamnese(
                patient_id,
                nucleo_id,
                operation,
                anamnese,
                operation_identity,
            )
            .await
    }

    pub async fn upsert(
        &self,
        dto: AcolhimentoAnamneseCreateUpdateDto,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoAnamneseRetrieveDto, AcolhimentoException> {
        let patient_id = required_uuid(dto.patient_id, ReasonCode::PatientIdRequired)?;

        let nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let existing = self.acolhimento_port.find_by_patient_id(patient_id).await?;
        let acolhimento_to_save = match existing {
            Some(curr) => curr.updated(dto.data_anamnese, dto.hora_anamnese, dto.observacoes),
            None => Acolhimento::create(
                patient_id,
                dto.data_anamnese,
                dto.hora_anamnese,
                dto.observacoes,
            ),
        };
        let updates = dto
            .secoes
            .into_iter()
            .map(|section| (section.nucleo_patient_id, section.anamnese))
            .collect();
        let saved_header = self
            .nucleo_patient_port
            .save_anamnese(acolhimento_to_save, updates, operation)
            .await?;

        let updated_nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;

        Ok(AcolhimentoAnamneseMapper::to_dto(
            &saved_header,
            &updated_nucleos,
        ))
    }
}
