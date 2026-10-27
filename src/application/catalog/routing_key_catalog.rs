pub struct RoutingKeyCatalog;

impl RoutingKeyCatalog {
    pub const PATIENT_RETRIEVE_V1: &'static str = "cmd.acolhimento.patient.retrieve.v1";
    pub const PATIENT_RETRIEVED_V1: &'static str = "ev.acolhimento.patient.retrieved.v1";
    pub const PATIENT_FAILED_V1: &'static str = "ev.acolhimento.patient.failed.v1";
    pub const STAFF_VINCULADO_V1: &'static str = "ev.acolhimento.staff.vinculado.v1";
    pub const STAFF_DESVINCULADO_V1: &'static str = "ev.acolhimento.staff.desvinculado.v1";
}
