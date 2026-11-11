use uuid::Uuid;

#[derive(PartialEq)]
pub(crate) struct NucleoPatientStaffEntity {
    pub id: Uuid,
    pub nucleo_patient_id: Uuid,
    pub staff_user_id: Uuid,
    pub role: String,
}

impl NucleoPatientStaffEntity {
    pub fn from_row(row: &tokio_postgres::Row) -> Result<Self, tokio_postgres::Error> {
        Ok(Self {
            id: row.try_get("id")?,
            nucleo_patient_id: row.try_get("nucleo_patient_id")?,
            staff_user_id: row.try_get("staff_user_id")?,
            role: row.try_get("role")?,
        })
    }
}
