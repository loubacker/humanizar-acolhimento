use crate::application::service::{
    AcolhimentoAnamneseService, AcolhimentoAvaliacaoService, AcolhimentoService,
};
use crate::domain::port::{
    AcolhimentoAvaliacaoPort, AcolhimentoPort, NucleoPatientPort, NucleoPatientStaffPort,
};
use crate::infrastructure::adapter::{
    AcolhimentoAdapter, AcolhimentoAvaliacaoAdapter, NucleoPatientAdapter,
    NucleoPatientStaffAdapter,
};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::persistence::repository::{
    AcolhimentoAvaliacaoRepository, AcolhimentoRepository, NucleoPatientRepository,
    NucleoPatientStaffRepository,
};
use std::sync::Arc;
use std::sync::atomic::AtomicBool;

#[derive(Clone)]
pub struct ApplicationState {
    pub(crate) acolhimento_service: Arc<AcolhimentoService>,
    pub(crate) acolhimento_anamnese_service: Arc<AcolhimentoAnamneseService>,
    pub(crate) acolhimento_avaliacao_service: Arc<AcolhimentoAvaliacaoService>,
    pub(crate) messaging_ready: Arc<AtomicBool>,
    pub(crate) database: Option<DatabaseConfig>,
}

impl ApplicationState {
    pub fn new(
        acolhimento_service: Arc<AcolhimentoService>,
        acolhimento_anamnese_service: Arc<AcolhimentoAnamneseService>,
        acolhimento_avaliacao_service: Arc<AcolhimentoAvaliacaoService>,
    ) -> Self {
        Self {
            acolhimento_service,
            acolhimento_anamnese_service,
            acolhimento_avaliacao_service,
            messaging_ready: Arc::new(AtomicBool::new(true)),
            database: None,
        }
    }

    pub fn from_database(
        database: DatabaseConfig,
        readiness: Arc<AtomicBool>,
        max_attempts: u32,
    ) -> Self {
        let health_database = database.clone();
        let nucleo_patient_port: Arc<dyn NucleoPatientPort> = Arc::new(NucleoPatientAdapter::new(
            NucleoPatientRepository::new(database.clone()),
        ));
        let nucleo_patient_staff_port: Arc<dyn NucleoPatientStaffPort> = Arc::new(
            NucleoPatientStaffAdapter::new(NucleoPatientStaffRepository::new(database.clone())),
        );
        let acolhimento_port: Arc<dyn AcolhimentoPort> = Arc::new(AcolhimentoAdapter::new(
            AcolhimentoRepository::new(database.clone()),
        ));
        let avaliacao_port: Arc<dyn AcolhimentoAvaliacaoPort> = Arc::new(
            AcolhimentoAvaliacaoAdapter::new(AcolhimentoAvaliacaoRepository::new(database)),
        );

        let acolhimento_service = Arc::new(AcolhimentoService::new(
            nucleo_patient_port.clone(),
            nucleo_patient_staff_port,
            acolhimento_port.clone(),
            max_attempts,
        ));
        let acolhimento_anamnese_service = Arc::new(AcolhimentoAnamneseService::new(
            acolhimento_port,
            nucleo_patient_port,
        ));
        let acolhimento_avaliacao_service =
            Arc::new(AcolhimentoAvaliacaoService::new(avaliacao_port));

        let mut state = Self::new(
            acolhimento_service,
            acolhimento_anamnese_service,
            acolhimento_avaliacao_service,
        );
        state.messaging_ready = readiness;
        state.database = Some(health_database);
        state
    }
}
