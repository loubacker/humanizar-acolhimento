use chrono::NaiveDateTime;
use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::OutboxEvent;
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::{NucleoPatientEntity, NucleoPatientStaffEntity};
use crate::infrastructure::persistence::mapper::MessagingEntityMapper;

pub(crate) struct NucleoPatientRepository {
    database: DatabaseConfig,
}

impl NucleoPatientRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        Self { database }
    }

    pub async fn find_by_patient_id(
        &self,
        patient_id: Uuid,
    ) -> Result<Vec<NucleoPatientEntity>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        let rows = client
            .query(
                r#"
                SELECT
                    id,
                    patient_id,
                    nucleo_id,
                    anamnese,
                    created_at,
                    updated_at
                FROM nucleo_patient
                WHERE patient_id = $1
                ORDER BY created_at ASC, id ASC
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?;
        rows.iter()
            .map(NucleoPatientEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)
    }

    pub async fn find_by_id(
        &self,
        id: Uuid,
    ) -> Result<Option<NucleoPatientEntity>, AcolhimentoException> {
        let client = self.database.acquire().await?;
        client
            .query_opt(
                r#"
                SELECT
                    id,
                    patient_id,
                    nucleo_id,
                    anamnese,
                    created_at,
                    updated_at
                FROM nucleo_patient
                WHERE id = $1
                "#,
                &[&id],
            )
            .await
            .map_err(database_error)?
            .as_ref()
            .map(NucleoPatientEntity::from_row)
            .transpose()
            .map_err(database_error)
    }

    pub async fn replace_patient_nucleos(
        &self,
        patient_id: Uuid,
        entries: Vec<(NucleoPatientEntity, Vec<NucleoPatientStaffEntity>)>,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<Vec<(NucleoPatientEntity, Vec<NucleoPatientStaffEntity>)>, AcolhimentoException>
    {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        transaction
            .execute(
                r#"
                DELETE FROM nucleo_patient
                WHERE patient_id = $1
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?;
        let mut saved_entries = Vec::with_capacity(entries.len());
        for (nucleo_entity, staff_members) in entries {
            let np_row = transaction
                .query_one(
                    r#"
                    INSERT INTO nucleo_patient (
                        id,
                        patient_id,
                        nucleo_id,
                        anamnese,
                        created_at,
                        updated_at
                    )
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING
                        id,
                        patient_id,
                        nucleo_id,
                        anamnese,
                        created_at,
                        updated_at
                    "#,
                    &[
                        &nucleo_entity.id,
                        &nucleo_entity.patient_id,
                        &nucleo_entity.nucleo_id,
                        &nucleo_entity.anamnese,
                        &nucleo_entity.created_at,
                        &nucleo_entity.updated_at,
                    ],
                )
                .await
                .map_err(database_error)?;
            let saved_np = NucleoPatientEntity::from_row(&np_row).map_err(database_error)?;
            let mut saved_staff = Vec::with_capacity(staff_members.len());
            for staff in staff_members {
                let staff_row = transaction
                    .query_one(
                        r#"
                        INSERT INTO nucleo_patient_staff (
                            id,
                            nucleo_patient_id,
                            staff_user_id,
                            role
                        )
                        VALUES ($1, $2, $3, $4)
                        RETURNING
                            id,
                            nucleo_patient_id,
                            staff_user_id,
                            role
                        "#,
                        &[&staff.id, &saved_np.id, &staff.staff_user_id, &staff.role],
                    )
                    .await
                    .map_err(database_error)?;
                saved_staff
                    .push(NucleoPatientStaffEntity::from_row(&staff_row).map_err(database_error)?);
            }
            saved_entries.push((saved_np, saved_staff));
        }
        Self::insert_outbox_events(&transaction, outbox_events).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(saved_entries)
    }

    pub async fn update_anamnese_batch(
        &self,
        updates: Vec<(Uuid, Option<Value>, NaiveDateTime)>,
    ) -> Result<Vec<NucleoPatientEntity>, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        let mut updated = Vec::with_capacity(updates.len());
        for (id, anamnese, updated_at) in updates {
            let row = transaction
                .query_opt(
                    r#"
                    UPDATE nucleo_patient
                    SET
                        anamnese = $2,
                        updated_at = $3
                    WHERE id = $1
                    RETURNING
                        id,
                        patient_id,
                        nucleo_id,
                        anamnese,
                        created_at,
                        updated_at
                    "#,
                    &[&id, &anamnese, &updated_at],
                )
                .await
                .map_err(database_error)?
                .ok_or_else(|| AcolhimentoException::new(ReasonCode::NucleoPatientNotFound))?;
            updated.push(NucleoPatientEntity::from_row(&row).map_err(database_error)?);
        }
        transaction.commit().await.map_err(database_error)?;
        Ok(updated)
    }

    pub async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<(), AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        let affected = transaction
            .execute(
                r#"
                DELETE FROM nucleo_patient
                WHERE patient_id = $1
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?;
        if affected == 0 {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }
        Self::insert_outbox_events(&transaction, outbox_events).await?;
        transaction.commit().await.map_err(database_error)
    }

    async fn insert_outbox_events(
        transaction: &tokio_postgres::Transaction<'_>,
        outbox_events: Vec<OutboxEvent>,
    ) -> Result<(), AcolhimentoException> {
        for event in outbox_events {
            let entity = MessagingEntityMapper::to_outbox_entity(event)?;
            let status = entity.status.as_str();
            transaction
                .execute(
                    r#"
                    INSERT INTO outbox_event (
                        event_id,
                        correlation_id,
                        producer_service,
                        exchange_name,
                        routing_key,
                        aggregate_type,
                        aggregate_id,
                        event_version,
                        payload,
                        actor_id,
                        user_agent,
                        origin_ip,
                        status,
                        attempt_count,
                        max_attempts,
                        next_retry_at,
                        last_error,
                        created_at,
                        published_at,
                        locked_by
                    )
                    VALUES (
                        $1, $2, $3, $4, $5, $6, $7, $8, $9, $10,
                        $11, $12, $13, $14, $15, $16, $17, $18, $19, $20
                    )
                    "#,
                    &[
                        &entity.event_id,
                        &entity.correlation_id,
                        &entity.producer_service,
                        &entity.exchange_name,
                        &entity.routing_key,
                        &entity.aggregate_type,
                        &entity.aggregate_id,
                        &entity.event_version,
                        &entity.payload,
                        &entity.actor_id,
                        &entity.user_agent,
                        &entity.origin_ip,
                        &status,
                        &entity.attempt_count,
                        &entity.max_attempts,
                        &entity.next_retry_at,
                        &entity.last_error,
                        &entity.created_at,
                        &entity.published_at,
                        &entity.locked_by,
                    ],
                )
                .await
                .map_err(database_error)?;
        }
        Ok(())
    }
}
