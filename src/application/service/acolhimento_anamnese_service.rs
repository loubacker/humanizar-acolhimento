use std::sync::Arc;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoAnamneseCreateUpdateDto, AcolhimentoAnamneseRetrieveDto, AnamneseItemDto,
};
use crate::application::http::mapper::AcolhimentoAnamneseMapper;
use crate::application::validator::required_uuid;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::Acolhimento;
use crate::domain::model::enums::ReasonCode;
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
            .unwrap_or_else(|| Acolhimento::create(patient_id, None, None, None));

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

    pub async fn upsert(
        &self,
        dto: AcolhimentoAnamneseCreateUpdateDto,
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
        let saved_header = self.acolhimento_port.upsert(acolhimento_to_save).await?;

        if !dto.secoes.is_empty() {
            let updates: Vec<(Uuid, Option<serde_json::Value>)> = dto
                .secoes
                .into_iter()
                .map(|s| (s.nucleo_patient_id, s.anamnese))
                .collect();
            self.nucleo_patient_port
                .update_anamnese_batch(updates)
                .await?;
        }

        let updated_nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;

        Ok(AcolhimentoAnamneseMapper::to_dto(
            &saved_header,
            &updated_nucleos,
        ))
    }

    pub async fn delete_by_patient_id(&self, patient_id: Uuid) -> Result<(), AcolhimentoException> {
        let nucleos = self
            .nucleo_patient_port
            .find_by_patient_id(patient_id)
            .await?;
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }

        let _ = self.acolhimento_port.delete_by_patient_id(patient_id).await;
        let updates: Vec<(Uuid, Option<serde_json::Value>)> =
            nucleos.into_iter().map(|n| (n.id(), None)).collect();
        self.nucleo_patient_port
            .update_anamnese_batch(updates)
            .await?;

        Ok(())
    }
}
