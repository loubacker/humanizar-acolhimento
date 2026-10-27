use chrono::{NaiveDate, NaiveDateTime, NaiveTime};
use uuid::Uuid;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Acolhimento {
    id: Uuid,
    patient_id: Uuid,
    data_anamnese: Option<NaiveDate>,
    hora_anamnese: Option<NaiveTime>,
    observacoes: Option<String>,
    created_at: NaiveDateTime,
    updated_at: NaiveDateTime,
}

impl Acolhimento {
    pub fn create(
        patient_id: Uuid,
        data_anamnese: Option<NaiveDate>,
        hora_anamnese: Option<NaiveTime>,
        observacoes: Option<String>,
    ) -> Self {
        let now = chrono::Local::now().naive_local();
        Self::restore(
            Uuid::new_v4(),
            patient_id,
            data_anamnese,
            hora_anamnese,
            observacoes,
            now,
            now,
        )
    }

    pub fn restore(
        id: Uuid,
        patient_id: Uuid,
        data_anamnese: Option<NaiveDate>,
        hora_anamnese: Option<NaiveTime>,
        observacoes: Option<String>,
        created_at: NaiveDateTime,
        updated_at: NaiveDateTime,
    ) -> Self {
        Self {
            id,
            patient_id,
            data_anamnese,
            hora_anamnese,
            observacoes,
            created_at,
            updated_at,
        }
    }

    pub fn updated(
        &self,
        data_anamnese: Option<NaiveDate>,
        hora_anamnese: Option<NaiveTime>,
        observacoes: Option<String>,
    ) -> Self {
        Self::restore(
            self.id,
            self.patient_id,
            data_anamnese,
            hora_anamnese,
            observacoes,
            self.created_at,
            chrono::Local::now().naive_local(),
        )
    }

    pub const fn id(&self) -> Uuid {
        self.id
    }

    pub const fn patient_id(&self) -> Uuid {
        self.patient_id
    }

    pub const fn data_anamnese(&self) -> Option<NaiveDate> {
        self.data_anamnese
    }

    pub const fn hora_anamnese(&self) -> Option<NaiveTime> {
        self.hora_anamnese
    }

    pub fn observacoes(&self) -> Option<&str> {
        self.observacoes.as_deref()
    }

    pub const fn created_at(&self) -> NaiveDateTime {
        self.created_at
    }

    pub const fn updated_at(&self) -> NaiveDateTime {
        self.updated_at
    }
}
