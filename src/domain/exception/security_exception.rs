use std::error::Error;
use std::fmt::{Display, Formatter};

use crate::domain::model::enums::ReasonCode;

use super::{AcolhimentoException, ReasonCodeException};

#[derive(Debug)]
pub struct SecurityException {
    exception: AcolhimentoException,
}

impl SecurityException {
    pub fn authentication() -> Self {
        Self::from_reason_code(ReasonCode::AuthenticationRequired)
    }

    pub fn authentication_with_source(source: impl Error + Send + Sync + 'static) -> Self {
        Self::from_reason_code_and_source(ReasonCode::AuthenticationRequired, source)
    }

    pub fn authorization() -> Self {
        Self::from_reason_code(ReasonCode::AuthorizationDenied)
    }

    pub fn authorization_with_source(source: impl Error + Send + Sync + 'static) -> Self {
        Self::from_reason_code_and_source(ReasonCode::AuthorizationDenied, source)
    }

    pub const fn reason_code(&self) -> ReasonCode {
        self.exception.reason_code()
    }

    pub fn message(&self) -> &str {
        self.exception.message()
    }

    fn from_reason_code(reason_code: ReasonCode) -> Self {
        Self {
            exception: AcolhimentoException::new(reason_code),
        }
    }

    fn from_reason_code_and_source(
        reason_code: ReasonCode,
        source: impl Error + Send + Sync + 'static,
    ) -> Self {
        Self {
            exception: AcolhimentoException::with_source(reason_code, source),
        }
    }
}

impl ReasonCodeException for SecurityException {
    fn reason_code(&self) -> ReasonCode {
        SecurityException::reason_code(self)
    }

    fn message(&self) -> &str {
        SecurityException::message(self)
    }
}

impl Display for SecurityException {
    fn fmt(&self, formatter: &mut Formatter<'_>) -> std::fmt::Result {
        Display::fmt(&self.exception, formatter)
    }
}

impl Error for SecurityException {
    fn source(&self) -> Option<&(dyn Error + 'static)> {
        self.exception.source()
    }
}

impl From<SecurityException> for AcolhimentoException {
    fn from(exception: SecurityException) -> Self {
        exception.exception
    }
}
