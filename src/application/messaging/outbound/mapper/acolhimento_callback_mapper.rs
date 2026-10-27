use crate::application::messaging::outbound::dto::{
    AcolhimentoRejectedDto, AcolhimentoRetrievedDto,
};
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::AcolhimentoRetrieveCommand;

pub struct AcolhimentoCallbackMapper;

impl AcolhimentoCallbackMapper {
    pub fn retrieved(
        command: &AcolhimentoRetrieveCommand,
        status: String,
    ) -> AcolhimentoRetrievedDto {
        AcolhimentoRetrievedDto {
            request_event_id: command.event_id,
            patient_id: command.patient_id,
            status,
        }
    }

    pub fn rejected(command: &AcolhimentoRetrieveCommand) -> AcolhimentoRejectedDto {
        AcolhimentoRejectedDto {
            request_event_id: command.event_id,
            patient_id: command.patient_id,
            reason_code: ReasonCode::AcolhimentoNotFound.code().to_owned(),
            message: ReasonCode::AcolhimentoNotFound.message().to_owned(),
        }
    }
}
