use std::str::FromStr;

use tokio_postgres::{Row, Transaction};
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::{
    OperationAction, OperationIdentity, OperationReservation, OperationState, OperationStatus,
};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;

#[derive(Clone)]
pub(crate) struct AcolhimentoOperationRepository {
    database: DatabaseConfig,
}

impl AcolhimentoOperationRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn reserve(
        &self,
        identity: &OperationIdentity,
    ) -> Result<OperationReservation, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let inserted = client.query_opt(
            "INSERT INTO acolhimento_operations (operation_id, actor_id, action, resource, request_hash, owner, state, lease_until) VALUES ($1, $2, $3, $4, $5, $6, 'PENDING', CURRENT_TIMESTAMP + INTERVAL '90 seconds') ON CONFLICT (operation_id) DO NOTHING RETURNING operation_id",
            &[&identity.id, &identity.actor_id, &identity.action.as_str(), &identity.resource, &identity.request_hash, &identity.owner],
        ).await.map_err(database_error)?;
        if inserted.is_some() {
            return Ok(OperationReservation::Acquired);
        }
        drop(client);
        self.expire(identity.id, identity.actor_id, identity.action)
            .await?;
        let client = self.database.acquire().await?;
        let row = client.query_opt(
            "SELECT actor_id, action, resource, request_hash, state FROM acolhimento_operations WHERE operation_id = $1",
            &[&identity.id],
        ).await.map_err(database_error)?.ok_or_else(|| AcolhimentoException::new(ReasonCode::OperationNotFound))?;
        let actor: Uuid = row.try_get("actor_id").map_err(database_error)?;
        if actor != identity.actor_id {
            return Err(AcolhimentoException::new(ReasonCode::OperationNotFound));
        }
        let action: String = row.try_get("action").map_err(database_error)?;
        let resource: String = row.try_get("resource").map_err(database_error)?;
        let hash: String = row.try_get("request_hash").map_err(database_error)?;
        if action != identity.action.as_str()
            || resource != identity.resource
            || hash != identity.request_hash
        {
            return Err(AcolhimentoException::new(ReasonCode::OperationConflict));
        }
        let state: String = row.try_get("state").map_err(database_error)?;
        match state.as_str() {
            "PENDING" => Ok(OperationReservation::Pending),
            "COMPLETED" => Ok(OperationReservation::Completed),
            "FAILED" => Ok(OperationReservation::Failed),
            _ => Err(AcolhimentoException::new(ReasonCode::PersistenceFailure)),
        }
    }

    pub async fn find(
        &self,
        id: Uuid,
        actor: Uuid,
        action: OperationAction,
    ) -> Result<Option<OperationStatus>, AcolhimentoException> {
        self.expire(id, actor, action).await?;
        let client = self.database.acquire().await?;
        let row = client.query_opt(
            "SELECT operation_id, action, state, reason_code FROM acolhimento_operations WHERE operation_id = $1 AND actor_id = $2 AND action = $3",
            &[&id, &actor, &action.as_str()],
        ).await.map_err(database_error)?;
        row.as_ref().map(to_status).transpose()
    }

    async fn expire(
        &self,
        id: Uuid,
        actor: Uuid,
        action: OperationAction,
    ) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        client.execute(
            "UPDATE acolhimento_operations SET state = 'FAILED', reason_code = 'OPERATION_EXPIRED', updated_at = CURRENT_TIMESTAMP WHERE operation_id = $1 AND actor_id = $2 AND action = $3 AND state = 'PENDING' AND lease_until < CURRENT_TIMESTAMP",
            &[&id, &actor, &action.as_str()],
        ).await.map_err(database_error)?;
        Ok(())
    }

    pub async fn record_response(
        &self,
        identity: &OperationIdentity,
        state: OperationState,
        status: u16,
        body: &[u8],
    ) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        let status = i16::try_from(status).map_err(|error| {
            AcolhimentoException::with_source(ReasonCode::UnexpectedError, error)
        })?;
        let affected = client.execute(
            "UPDATE acolhimento_operations SET response_status = $3, response_body = $4, updated_at = CURRENT_TIMESTAMP WHERE operation_id = $1 AND owner = $2 AND state = $5 AND response_body IS NULL",
            &[&identity.id, &identity.owner, &status, &body, &state.as_str()],
        ).await.map_err(database_error)?;
        if affected != 1 {
            return Err(AcolhimentoException::new(ReasonCode::OperationLeaseLost));
        }
        Ok(())
    }

    pub async fn fail(
        &self,
        identity: &OperationIdentity,
        reason_code: &str,
    ) -> Result<(), AcolhimentoException> {
        let client = self.database.acquire().await?;
        client.execute(
            "UPDATE acolhimento_operations SET state = 'FAILED', reason_code = $3, updated_at = CURRENT_TIMESTAMP WHERE operation_id = $1 AND owner = $2 AND state = 'PENDING'",
            &[&identity.id, &identity.owner, &reason_code],
        ).await.map_err(database_error)?;
        Ok(())
    }

    pub async fn snapshot(
        &self,
        identity: &OperationIdentity,
        state: OperationState,
    ) -> Result<Option<(u16, Vec<u8>)>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let row = client.query_opt(
            "SELECT response_status, response_body FROM acolhimento_operations WHERE operation_id = $1 AND actor_id = $2 AND action = $3 AND resource = $4 AND request_hash = $5 AND state = $6",
            &[&identity.id, &identity.actor_id, &identity.action.as_str(), &identity.resource, &identity.request_hash, &state.as_str()],
        ).await.map_err(database_error)?.ok_or_else(|| AcolhimentoException::new(ReasonCode::OperationNotFound))?;
        response_snapshot(&row)
    }

    pub async fn lock(
        transaction: &Transaction<'_>,
        identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let row = transaction.query_opt(
            "SELECT operation_id FROM acolhimento_operations WHERE operation_id = $1 AND actor_id = $2 AND action = $3 AND owner = $4 AND state = 'PENDING' AND lease_until > clock_timestamp() FOR UPDATE",
            &[&identity.id, &identity.actor_id, &identity.action.as_str(), &identity.owner],
        ).await.map_err(database_error)?;
        if row.is_none() {
            return Err(AcolhimentoException::new(ReasonCode::OperationLeaseLost));
        }
        Ok(())
    }

    pub async fn complete(
        transaction: &Transaction<'_>,
        identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let affected = transaction.execute(
            "UPDATE acolhimento_operations SET state = 'COMPLETED', updated_at = CURRENT_TIMESTAMP WHERE operation_id = $1 AND owner = $2 AND state = 'PENDING' AND lease_until > clock_timestamp()",
            &[&identity.id, &identity.owner],
        ).await.map_err(database_error)?;
        if affected != 1 {
            return Err(AcolhimentoException::new(ReasonCode::OperationLeaseLost));
        }
        Ok(())
    }
}

fn response_snapshot(row: &Row) -> Result<Option<(u16, Vec<u8>)>, AcolhimentoException> {
    let status: Option<i16> = row.try_get("response_status").map_err(database_error)?;
    let body: Option<Vec<u8>> = row.try_get("response_body").map_err(database_error)?;
    match (status, body) {
        (None, None) => Ok(None),
        (Some(status), Some(body)) => Ok(Some((
            u16::try_from(status).map_err(|error| {
                AcolhimentoException::with_source(ReasonCode::PersistenceFailure, error)
            })?,
            body,
        ))),
        _ => Err(AcolhimentoException::new(ReasonCode::PersistenceFailure)),
    }
}

fn to_status(row: &Row) -> Result<OperationStatus, AcolhimentoException> {
    let action: String = row.try_get("action").map_err(database_error)?;
    let state: String = row.try_get("state").map_err(database_error)?;
    let action = OperationAction::from_str(&action)
        .map_err(|_| AcolhimentoException::new(ReasonCode::PersistenceFailure))?;
    let state = match state.as_str() {
        "PENDING" => OperationState::Pending,
        "COMPLETED" => OperationState::Completed,
        "FAILED" => OperationState::Failed,
        _ => return Err(AcolhimentoException::new(ReasonCode::PersistenceFailure)),
    };
    Ok(OperationStatus {
        id: row.try_get("operation_id").map_err(database_error)?,
        action,
        state,
        reason_code: row.try_get("reason_code").map_err(database_error)?,
    })
}
