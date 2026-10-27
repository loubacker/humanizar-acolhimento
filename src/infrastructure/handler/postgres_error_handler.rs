use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use tokio_postgres::error::SqlState;

pub(crate) fn database_error(error: tokio_postgres::Error) -> AcolhimentoException {
    let reason = match error.as_db_error() {
        Some(details)
            if details.code() == &SqlState::UNIQUE_VIOLATION
                && details.constraint() == Some("uk_nucleo_patient_patient_nucleo") =>
        {
            ReasonCode::AcolhimentoAlreadyExists
        }
        Some(details)
            if details.code() == &SqlState::UNIQUE_VIOLATION
                && details.constraint() == Some("uk_acolhimento_avaliacao_patient_avaliacao") =>
        {
            ReasonCode::AcolhimentoAvaliacaoAlreadyExists
        }
        _ => ReasonCode::DatabaseAccessFailed,
    };
    let transient = error.is_closed()
        || error.code().is_some_and(|code| {
            matches!(code.code(), "40001" | "40P01" | "57P01" | "57P02" | "57P03")
                || code.code().starts_with("08")
        });
    AcolhimentoException::with_retryable_source(reason, transient, error)
}
