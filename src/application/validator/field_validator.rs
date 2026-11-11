use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;

pub(crate) fn required(
    value: Option<String>,
    reason: ReasonCode,
) -> Result<String, AcolhimentoException> {
    let value = value.ok_or_else(|| AcolhimentoException::new(reason))?;
    let trimmed = value.trim();
    if trimmed.is_empty() {
        return Err(AcolhimentoException::new(reason));
    }
    Ok(trimmed.to_owned())
}

pub(crate) fn required_uuid(
    value: Option<Uuid>,
    reason: ReasonCode,
) -> Result<Uuid, AcolhimentoException> {
    value
        .filter(|id| !id.is_nil())
        .ok_or_else(|| AcolhimentoException::new(reason))
}
