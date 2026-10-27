use chrono::{DateTime, Utc};
use serde::Serialize;

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ErrorResponseDto {
    pub timestamp: DateTime<Utc>,
    pub status: u16,
    pub error: String,
    pub reason_code: String,
    pub message: String,
    pub path: String,
    pub correlation_id: String,
}
