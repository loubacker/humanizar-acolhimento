use crate::application::http::dto::{AcolhimentoAnamneseRetrieveDto, AnamneseItemDto};
use crate::domain::model::{Acolhimento, NucleoPatient};

pub struct AcolhimentoAnamneseMapper;

impl AcolhimentoAnamneseMapper {
    pub fn to_dto(
        acolhimento: &Acolhimento,
        nucleos: &[NucleoPatient],
    ) -> AcolhimentoAnamneseRetrieveDto {
        let secoes = nucleos
            .iter()
            .map(|n| AnamneseItemDto {
                nucleo_patient_id: n.id(),
                nucleo_id: n.nucleo_id(),
                anamnese: n.anamnese().cloned(),
            })
            .collect();

        AcolhimentoAnamneseRetrieveDto {
            id: acolhimento.id(),
            patient_id: acolhimento.patient_id(),
            data_anamnese: acolhimento.data_anamnese(),
            hora_anamnese: acolhimento.hora_anamnese(),
            observacoes: acolhimento.observacoes().map(ToOwned::to_owned),
            created_at: acolhimento.created_at(),
            updated_at: acolhimento.updated_at(),
            secoes,
        }
    }
}
