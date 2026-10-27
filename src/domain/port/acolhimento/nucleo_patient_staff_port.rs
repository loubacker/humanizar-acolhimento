use async_trait::async_trait;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::NucleoPatientStaff;

#[async_trait]
pub trait NucleoPatientStaffPort: Send + Sync {
    async fn find_by_nucleo_patient_ids(
        &self,
        nucleo_patient_ids: &[Uuid],
    ) -> Result<Vec<NucleoPatientStaff>, AcolhimentoException>;
}
