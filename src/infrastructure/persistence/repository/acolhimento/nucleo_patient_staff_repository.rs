use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::NucleoPatientStaffEntity;

pub(crate) struct NucleoPatientStaffRepository {
    database: DatabaseConfig,
}

impl NucleoPatientStaffRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn find_by_nucleo_patient_ids(
        &self,
        nucleo_patient_ids: &[Uuid],
    ) -> Result<Vec<NucleoPatientStaffEntity>, AcolhimentoException> {
        if nucleo_patient_ids.is_empty() {
            return Ok(Vec::new());
        }
        let client = self.database.acquire().await?;
        let rows = client
            .query(
                r#"
                SELECT
                    id,
                    nucleo_patient_id,
                    staff_user_id,
                    role
                FROM nucleo_patient_staff
                WHERE nucleo_patient_id = ANY($1)
                    AND EXISTS (SELECT 1 FROM nucleo_patient np WHERE np.id = nucleo_patient_id AND np.desvinculado_at IS NULL)
                ORDER BY id ASC
                "#,
                &[&nucleo_patient_ids],
            )
            .await
            .map_err(database_error)?;
        rows.iter()
            .map(NucleoPatientStaffEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)
    }
}
