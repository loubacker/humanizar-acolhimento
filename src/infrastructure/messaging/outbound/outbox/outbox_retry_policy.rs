use std::time::Duration;

#[derive(Clone, Copy)]
pub struct OutboxRetryPolicy {
    initial: Duration,
    maximum: Duration,
}

impl Default for OutboxRetryPolicy {
    fn default() -> Self {
        Self {
            initial: Duration::from_millis(2000),
            maximum: Duration::from_millis(300000),
        }
    }
}

impl OutboxRetryPolicy {
    pub fn new(
        initial: Duration,
        maximum: Duration,
    ) -> Result<Self, crate::domain::exception::TechnicalError> {
        if initial.is_zero() || initial > maximum || maximum > Duration::from_secs(86400) {
            return Err(crate::domain::exception::TechnicalError::new(
                "Retry inicial/maximo invalidos",
            ));
        }
        Ok(Self { initial, maximum })
    }

    pub fn delay(self, attempt: u32) -> Duration {
        let factor = 1_u32.checked_shl(attempt).unwrap_or(u32::MAX);
        self.initial
            .checked_mul(factor)
            .unwrap_or(self.maximum)
            .min(self.maximum)
    }
}
