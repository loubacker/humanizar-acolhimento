use serde::Deserialize;

use super::AvaliacaoPacienteReferenceDto;

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AvaliacaoPacienteReferencePageDto {
    pub items: Vec<AvaliacaoPacienteReferenceDto>,
    pub total: u64,
    pub page: u32,
    pub size: u32,
}
