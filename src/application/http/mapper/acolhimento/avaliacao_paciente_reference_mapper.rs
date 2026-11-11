use crate::application::http::dto::AvaliacaoPacienteReferenceDto;
use crate::domain::model::AvaliacaoPacienteReference;

pub struct AvaliacaoPacienteReferenceMapper;

impl AvaliacaoPacienteReferenceMapper {
    pub fn to_domain(dto: AvaliacaoPacienteReferenceDto) -> AvaliacaoPacienteReference {
        AvaliacaoPacienteReference {
            id: dto.id,
            patient_id: dto.patient_id,
            titulo: dto.titulo_avaliacao,
            modalidade: dto.modalidade,
            status: dto.status,
        }
    }
}
