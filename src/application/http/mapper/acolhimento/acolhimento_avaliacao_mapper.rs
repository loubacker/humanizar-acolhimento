use crate::application::http::dto::AcolhimentoAvaliacaoRetrieveDto;
use crate::domain::model::AcolhimentoAvaliacao;

pub struct AcolhimentoAvaliacaoMapper;

impl AcolhimentoAvaliacaoMapper {
    pub fn to_dto(model: &AcolhimentoAvaliacao) -> AcolhimentoAvaliacaoRetrieveDto {
        AcolhimentoAvaliacaoRetrieveDto {
            id: model.id(),
            patient_id: model.patient_id(),
            avaliacao_paciente_id: model.avaliacao_paciente_id(),
            titulo_avaliacao: model.titulo_avaliacao().to_owned(),
            modalidade: model.modalidade().map(ToOwned::to_owned),
            status: model.status().map(ToOwned::to_owned),
            created_at: model.created_at(),
        }
    }
}
