use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::NucleoPatientStaff;
use crate::domain::port::NucleoPatientStaffPort;
use crate::infrastructure::persistence::repository::NucleoPatientStaffRepository;

pub(crate) struct NucleoPatientStaffAdapter {
    repository: NucleoPatientStaffRepository,
}

impl NucleoPatientStaffAdapter {
    pub fn new(repository: NucleoPatientStaffRepository) -> Self {
        Self { repository }
    }
}

#[async_trait]
impl NucleoPatientStaffPort for NucleoPatientStaffAdapter {
    async fn find_by_nucleo_patient_ids(
        &self,
        nucleo_patient_ids: &[Uuid],
    ) -> Result<Vec<NucleoPatientStaff>, AcolhimentoException> {
        let rows = self
            .repository
            .find_by_nucleo_patient_ids(nucleo_patient_ids)
            .await?;
        Ok(rows
            .into_iter()
            .map(|e| {
                NucleoPatientStaff::restore(e.id, e.nucleo_patient_id, e.staff_user_id, e.role)
            })
            .collect())
    }
}
