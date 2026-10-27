use chrono::NaiveDateTime;
use serde_json::Value;
use uuid::Uuid;

#[derive(Debug, Clone, PartialEq)]
pub struct NucleoPatient {
    id: Uuid,
    patient_id: Uuid,
    nucleo_id: Uuid,
    anamnese: Option<Value>,
    created_at: Option<NaiveDateTime>,
    updated_at: Option<NaiveDateTime>,
}

impl NucleoPatient {
    pub fn create(patient_id: Uuid, nucleo_id: Uuid) -> Self {
        let now = chrono::Local::now().naive_local();
        Self::restore(
            Uuid::new_v4(),
            patient_id,
            nucleo_id,
            None,
            Some(now),
            Some(now),
        )
    }

    pub fn restore(
        id: Uuid,
        patient_id: Uuid,
        nucleo_id: Uuid,
        anamnese: Option<Value>,
        created_at: Option<NaiveDateTime>,
        updated_at: Option<NaiveDateTime>,
    ) -> Self {
        Self {
            id,
            patient_id,
            nucleo_id,
            anamnese,
            created_at,
            updated_at,
        }
    }

    pub fn with_anamnese(&self, anamnese: Option<Value>) -> Self {
        Self::restore(
            self.id,
            self.patient_id,
            self.nucleo_id,
            anamnese,
            self.created_at,
            Some(chrono::Local::now().naive_local()),
        )
    }

    pub const fn id(&self) -> Uuid {
        self.id
    }

    pub const fn patient_id(&self) -> Uuid {
        self.patient_id
    }

    pub const fn nucleo_id(&self) -> Uuid {
        self.nucleo_id
    }

    pub const fn anamnese(&self) -> Option<&Value> {
        self.anamnese.as_ref()
    }

    pub const fn created_at(&self) -> Option<NaiveDateTime> {
        self.created_at
    }

    pub const fn updated_at(&self) -> Option<NaiveDateTime> {
        self.updated_at
    }
}
