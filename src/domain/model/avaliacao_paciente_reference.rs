use uuid::Uuid;

#[derive(Clone)]
pub struct AvaliacaoPacienteReference {
    pub id: Uuid,
    pub patient_id: Uuid,
    pub titulo: Option<String>,
    pub modalidade: String,
    pub status: String,
}
