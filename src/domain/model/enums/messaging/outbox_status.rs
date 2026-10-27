#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OutboxStatus {
    New,
    Locked,
    Published,
    Failed,
    Dead,
}
