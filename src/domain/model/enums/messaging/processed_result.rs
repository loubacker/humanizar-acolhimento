#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ProcessedResult {
    Success,
    Ignored,
    Failed,
}
