use chrono::NaiveDateTime;
use uuid::Uuid;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AcolhimentoAvaliacao {
    id: Uuid,
    patient_id: Uuid,
    avaliacao_paciente_id: Uuid,
    titulo_avaliacao: String,
    modalidade: Option<String>,
    status: Option<String>,
    created_at: NaiveDateTime,
}

impl AcolhimentoAvaliacao {
    pub fn create(
        patient_id: Uuid,
        avaliacao_paciente_id: Uuid,
        titulo_avaliacao: String,
        modalidade: Option<String>,
        status: Option<String>,
    ) -> Self {
        let now = chrono::Local::now().naive_local();
        Self::restore(
            Uuid::new_v4(),
            patient_id,
            avaliacao_paciente_id,
            titulo_avaliacao,
            modalidade,
            status,
            now,
        )
    }

    pub fn restore(
        id: Uuid,
        patient_id: Uuid,
        avaliacao_paciente_id: Uuid,
        titulo_avaliacao: String,
        modalidade: Option<String>,
        status: Option<String>,
        created_at: NaiveDateTime,
    ) -> Self {
        Self {
            id,
            patient_id,
            avaliacao_paciente_id,
            titulo_avaliacao,
            modalidade,
            status,
            created_at,
        }
    }

    pub const fn id(&self) -> Uuid {
        self.id
    }

    pub const fn patient_id(&self) -> Uuid {
        self.patient_id
    }

    pub const fn avaliacao_paciente_id(&self) -> Uuid {
        self.avaliacao_paciente_id
    }

    pub fn titulo_avaliacao(&self) -> &str {
        &self.titulo_avaliacao
    }

    pub fn modalidade(&self) -> Option<&str> {
        self.modalidade.as_deref()
    }

    pub fn status(&self) -> Option<&str> {
        self.status.as_deref()
    }

    pub const fn created_at(&self) -> NaiveDateTime {
        self.created_at
    }
}
