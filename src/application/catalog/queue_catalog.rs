pub struct QueueCatalog;

impl QueueCatalog {
    pub const PATIENT_RETRIEVE: &'static str = "humanizar.acolhimento.patient.retrieve";
    pub const PATIENT_RETRIEVE_DLQ: &'static str = "humanizar.acolhimento.patient.retrieve.dlq";
}
