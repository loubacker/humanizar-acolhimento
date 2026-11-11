use std::str::FromStr;

use uuid::Uuid;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OperationAction {
    AcolhimentoCriar,
    AcolhimentoAtualizar,
    AcolhimentoExcluir,
    AnamneseRegistrar,
    AnamneseAtualizar,
    AnamneseExcluir,
    AvaliacaoVincular,
    AvaliacaoDesvincular,
}

impl OperationAction {
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::AcolhimentoCriar => "ACOLHIMENTO_CRIAR",
            Self::AcolhimentoAtualizar => "ACOLHIMENTO_ATUALIZAR",
            Self::AcolhimentoExcluir => "ACOLHIMENTO_EXCLUIR",
            Self::AnamneseRegistrar => "ANAMNESE_REGISTRAR",
            Self::AnamneseAtualizar => "ANAMNESE_ATUALIZAR",
            Self::AnamneseExcluir => "ANAMNESE_EXCLUIR",
            Self::AvaliacaoVincular => "AVALIACAO_VINCULAR",
            Self::AvaliacaoDesvincular => "AVALIACAO_DESVINCULAR",
        }
    }
}

impl FromStr for OperationAction {
    type Err = ();

    fn from_str(value: &str) -> Result<Self, Self::Err> {
        match value {
            "ACOLHIMENTO_CRIAR" => Ok(Self::AcolhimentoCriar),
            "ACOLHIMENTO_ATUALIZAR" => Ok(Self::AcolhimentoAtualizar),
            "ACOLHIMENTO_EXCLUIR" => Ok(Self::AcolhimentoExcluir),
            "ANAMNESE_REGISTRAR" => Ok(Self::AnamneseRegistrar),
            "ANAMNESE_ATUALIZAR" => Ok(Self::AnamneseAtualizar),
            "ANAMNESE_EXCLUIR" => Ok(Self::AnamneseExcluir),
            "AVALIACAO_VINCULAR" => Ok(Self::AvaliacaoVincular),
            "AVALIACAO_DESVINCULAR" => Ok(Self::AvaliacaoDesvincular),
            _ => Err(()),
        }
    }
}

#[derive(Clone)]
pub struct OperationIdentity {
    pub id: Uuid,
    pub actor_id: Uuid,
    pub action: OperationAction,
    pub resource: String,
    pub request_hash: String,
    pub owner: Uuid,
}

#[derive(Clone, Copy, PartialEq, Eq)]
pub enum OperationState {
    Pending,
    Completed,
    Failed,
}

impl OperationState {
    pub const fn as_str(self) -> &'static str {
        match self {
            Self::Pending => "PENDING",
            Self::Completed => "COMPLETED",
            Self::Failed => "FAILED",
        }
    }
}

pub struct OperationStatus {
    pub id: Uuid,
    pub action: OperationAction,
    pub state: OperationState,
    pub reason_code: Option<String>,
}

pub enum OperationReservation {
    Acquired,
    Pending,
    Completed,
    Failed,
}
