use uuid::Uuid;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct NucleoPatientStaff {
    id: Uuid,
    nucleo_patient_id: Uuid,
    staff_user_id: Uuid,
    role: String,
}

impl NucleoPatientStaff {
    pub fn create(nucleo_patient_id: Uuid, staff_user_id: Uuid, role: String) -> Self {
        Self::restore(Uuid::new_v4(), nucleo_patient_id, staff_user_id, role)
    }

    pub fn restore(id: Uuid, nucleo_patient_id: Uuid, staff_user_id: Uuid, role: String) -> Self {
        Self {
            id,
            nucleo_patient_id,
            staff_user_id,
            role,
        }
    }

    pub const fn id(&self) -> Uuid {
        self.id
    }

    pub const fn nucleo_patient_id(&self) -> Uuid {
        self.nucleo_patient_id
    }

    pub const fn staff_user_id(&self) -> Uuid {
        self.staff_user_id
    }

    pub fn role(&self) -> &str {
        &self.role
    }
}
