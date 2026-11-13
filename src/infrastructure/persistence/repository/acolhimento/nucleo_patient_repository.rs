use serde_json::Value;
use std::collections::HashSet;
use uuid::Uuid;

use super::AcolhimentoOperationRepository;
use crate::domain::exception::AcolhimentoException;
use crate::domain::model::enums::ReasonCode;
use crate::domain::model::messaging::OutboxEvent;
use crate::domain::model::{AnamneseOperation, OperationIdentity};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::{
    AcolhimentoEntity, NucleoPatientEntity, NucleoPatientStaffEntity,
};
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
                WHERE patient_id = $1 AND desvinculado_at IS NULL
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
                WHERE id = $1 AND desvinculado_at IS NULL
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
        expected_nucleos: Vec<NucleoPatientEntity>,
        expected_staff: Vec<NucleoPatientStaffEntity>,
        entries: Vec<(NucleoPatientEntity, Vec<NucleoPatientStaffEntity>)>,
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
    ) -> Result<Vec<(NucleoPatientEntity, Vec<NucleoPatientStaffEntity>)>, AcolhimentoException>
    {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation).await?;
        Self::lock_patient(&transaction, patient_id).await?;
        Self::verify_snapshot(&transaction, patient_id, expected_nucleos, expected_staff).await?;
        let desired_ids: Vec<Uuid> = entries.iter().map(|(nucleo, _)| nucleo.id).collect();
        transaction
            .execute(
                r#"
                UPDATE nucleo_patient
                SET
                    desvinculado_at = CURRENT_TIMESTAMP,
                    desvinculado_por = $2,
                    desvinculo_operation_id = $3
                WHERE patient_id = $1
                    AND desvinculado_at IS NULL
                    AND NOT (id = ANY($4))
                "#,
                &[
                    &patient_id,
                    &operation.actor_id,
                    &operation.id,
                    &desired_ids,
                ],
            )
            .await
            .map_err(database_error)?;
        let mut saved_entries = Vec::with_capacity(entries.len());
        for (nucleo, staff_members) in entries {
            let existing = transaction
                .query_opt(
                    r#"
                SELECT id
                    FROM nucleo_patient
                    WHERE id = $1
                        AND patient_id = $2
                        AND desvinculado_at IS NULL
                "#,
                    &[&nucleo.id, &patient_id],
                )
                .await
                .map_err(database_error)?;
            let row = if existing.is_some() {
                transaction
                    .query_one(
                        r#"
                UPDATE nucleo_patient
                        SET updated_at = CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo'
                        WHERE id = $1
                            AND patient_id = $2
                            AND desvinculado_at IS NULL
                        RETURNING id, patient_id, nucleo_id, anamnese, created_at, updated_at
                "#,
                        &[&nucleo.id, &patient_id],
                    )
                    .await
            } else {
                transaction
                    .query_one(
                        r#"
                INSERT INTO nucleo_patient (
                            id, patient_id, nucleo_id, anamnese, created_at, updated_at
                        )
                        VALUES ($1, $2, $3, $4, $5, $6)
                        RETURNING id, patient_id, nucleo_id, anamnese, created_at, updated_at
                "#,
                        &[
                            &nucleo.id,
                            &nucleo.patient_id,
                            &nucleo.nucleo_id,
                            &nucleo.anamnese,
                            &nucleo.created_at,
                            &nucleo.updated_at,
                        ],
                    )
                    .await
            }
            .map_err(database_error)?;
            let saved_np = NucleoPatientEntity::from_row(&row).map_err(database_error)?;
            let desired_staff: Vec<Uuid> = staff_members.iter().map(|staff| staff.id).collect();
            transaction
                .execute(
                    r#"
                DELETE FROM nucleo_patient_staff
                    WHERE nucleo_patient_id = $1
                        AND NOT (id = ANY($2))
                "#,
                    &[&saved_np.id, &desired_staff],
                )
                .await
                .map_err(database_error)?;
            let mut saved_staff = Vec::with_capacity(staff_members.len());
            for staff in staff_members {
                let row = transaction
                    .query_one(
                        r#"
                INSERT INTO nucleo_patient_staff (id, nucleo_patient_id, staff_user_id, role)
                        VALUES ($1, $2, $3, $4)
                        ON CONFLICT (id) DO UPDATE
                        SET role = EXCLUDED.role
                        WHERE nucleo_patient_staff.nucleo_patient_id = EXCLUDED.nucleo_patient_id
                            AND nucleo_patient_staff.staff_user_id = EXCLUDED.staff_user_id
                        RETURNING id, nucleo_patient_id, staff_user_id, role
                "#,
                        &[&staff.id, &saved_np.id, &staff.staff_user_id, &staff.role],
                    )
                    .await
                    .map_err(database_error)?;
                saved_staff.push(NucleoPatientStaffEntity::from_row(&row).map_err(database_error)?);
            }
            saved_entries.push((saved_np, saved_staff));
        }
        Self::insert_outbox_events(&transaction, outbox_events).await?;
        AcolhimentoOperationRepository::complete(&transaction, operation).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(saved_entries)
    }

    pub async fn save_anamnese(
        &self,
        acolhimento: AcolhimentoEntity,
        updates: Vec<(Uuid, Option<Value>)>,
        operation: &OperationIdentity,
    ) -> Result<AcolhimentoEntity, AcolhimentoException> {
        let mut seen = HashSet::with_capacity(updates.len());
        if updates.iter().any(|(id, _)| !seen.insert(*id)) {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation).await?;
        Self::lock_patient(&transaction, acolhimento.patient_id).await?;
        let nucleos = transaction
            .query(
                "SELECT id FROM nucleo_patient WHERE patient_id = $1 ORDER BY id FOR UPDATE",
                &[&acolhimento.patient_id],
            )
            .await
            .map_err(database_error)?;
        if nucleos.is_empty() {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }
        for (id, anamnese) in updates {
            let changed = transaction
                .execute(
                    r#"
                    UPDATE nucleo_patient
                    SET
                        anamnese = $2,
                        updated_at = $3
                    WHERE id = $1 AND patient_id = $4
                    "#,
                    &[
                        &id,
                        &anamnese,
                        &acolhimento.updated_at,
                        &acolhimento.patient_id,
                    ],
                )
                .await
                .map_err(database_error)?;
            if changed != 1 {
                return Err(AcolhimentoException::new(ReasonCode::NucleoPatientNotFound));
            }
        }
        let row = transaction
            .query_one(
                r#"
                INSERT INTO acolhimento (
                    id, patient_id, data_anamnese, hora_anamnese, observacoes, created_at, updated_at
                ) VALUES ($1, $2, $3, $4, $5, $6, $7)
                ON CONFLICT (patient_id) DO UPDATE SET
                    data_anamnese = EXCLUDED.data_anamnese,
                    hora_anamnese = EXCLUDED.hora_anamnese,
                    observacoes = EXCLUDED.observacoes,
                    updated_at = EXCLUDED.updated_at
                RETURNING id, patient_id, data_anamnese, hora_anamnese, observacoes, created_at, updated_at
                "#,
                &[
                    &acolhimento.id,
                    &acolhimento.patient_id,
                    &acolhimento.data_anamnese,
                    &acolhimento.hora_anamnese,
                    &acolhimento.observacoes,
                    &acolhimento.created_at,
                    &acolhimento.updated_at,
                ],
            )
            .await
            .map_err(database_error)?;
        let saved = AcolhimentoEntity::from_row(&row).map_err(database_error)?;
        AcolhimentoOperationRepository::complete(&transaction, operation).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok(saved)
    }

    pub async fn delete_by_patient_id(
        &self,
        patient_id: Uuid,
        expected_nucleos: Vec<NucleoPatientEntity>,
        expected_staff: Vec<NucleoPatientStaffEntity>,
        outbox_events: Vec<OutboxEvent>,
        operation: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation).await?;
        Self::lock_patient(&transaction, patient_id).await?;
        Self::verify_snapshot(&transaction, patient_id, expected_nucleos, expected_staff).await?;
        let affected = transaction
            .execute(
                r#"
                UPDATE nucleo_patient
                SET
                    desvinculado_at = CURRENT_TIMESTAMP,
                    desvinculado_por = $2,
                    desvinculo_operation_id = $3
                WHERE patient_id = $1
                    AND desvinculado_at IS NULL
                "#,
                &[&patient_id, &operation.actor_id, &operation.id],
            )
            .await
            .map_err(database_error)?;
        if affected == 0 {
            return Err(AcolhimentoException::new(ReasonCode::AcolhimentoNotFound));
        }
        Self::insert_outbox_events(&transaction, outbox_events).await?;
        AcolhimentoOperationRepository::complete(&transaction, operation).await?;
        transaction.commit().await.map_err(database_error)
    }

    pub async fn change_nucleo_anamnese(
        &self,
        patient_id: Uuid,
        nucleo_id: Uuid,
        operation: AnamneseOperation,
        anamnese: Option<Value>,
        operation_identity: &OperationIdentity,
    ) -> Result<(), AcolhimentoException> {
        if matches!(operation, AnamneseOperation::Delete) != anamnese.is_none() {
            return Err(AcolhimentoException::new(ReasonCode::ValidationError));
        }
        let mut client = self.database.acquire().await?;
        let transaction = client.transaction().await.map_err(database_error)?;
        AcolhimentoOperationRepository::lock(&transaction, operation_identity).await?;
        Self::lock_patient(&transaction, patient_id).await?;
        let row = transaction
            .query_opt(
                "SELECT anamnese FROM nucleo_patient WHERE patient_id = $1 AND nucleo_id = $2 FOR UPDATE",
                &[&patient_id, &nucleo_id],
            )
            .await
            .map_err(database_error)?
            .ok_or_else(|| AcolhimentoException::new(ReasonCode::NucleoPatientNotFound))?;
        let existing: Option<Value> = row.try_get("anamnese").map_err(database_error)?;
        match (operation, existing.is_some()) {
            (AnamneseOperation::Create, true) => {
                return Err(AcolhimentoException::new(ReasonCode::AnamneseAlreadyExists));
            }
            (AnamneseOperation::Update | AnamneseOperation::Delete, false) => {
                return Err(AcolhimentoException::new(ReasonCode::AnamneseNotFound));
            }
            _ => {}
        }
        transaction
            .execute(
                "UPDATE nucleo_patient SET anamnese = $3, updated_at = CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo' WHERE patient_id = $1 AND nucleo_id = $2",
                &[&patient_id, &nucleo_id, &anamnese],
            )
            .await
            .map_err(database_error)?;
        AcolhimentoOperationRepository::complete(&transaction, operation_identity).await?;
        transaction.commit().await.map_err(database_error)
    }

    pub(crate) async fn lock_patient(
        transaction: &tokio_postgres::Transaction<'_>,
        patient_id: Uuid,
    ) -> Result<(), AcolhimentoException> {
        transaction
            .query_one(
                "SELECT pg_advisory_xact_lock(hashtextextended($1::text, 0))",
                &[&patient_id.to_string()],
            )
            .await
            .map_err(database_error)?;
        Ok(())
    }

    async fn verify_snapshot(
        transaction: &tokio_postgres::Transaction<'_>,
        patient_id: Uuid,
        mut expected_nucleos: Vec<NucleoPatientEntity>,
        mut expected_staff: Vec<NucleoPatientStaffEntity>,
    ) -> Result<(), AcolhimentoException> {
        let rows = transaction.query(
            "SELECT id, patient_id, nucleo_id, anamnese, created_at, updated_at FROM nucleo_patient WHERE patient_id = $1 AND desvinculado_at IS NULL ORDER BY id FOR UPDATE",
            &[&patient_id],
        ).await.map_err(database_error)?;
        let current_nucleos = rows
            .iter()
            .map(NucleoPatientEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        let rows = transaction.query(
            "SELECT staff.id, staff.nucleo_patient_id, staff.staff_user_id, staff.role FROM nucleo_patient_staff staff JOIN nucleo_patient np ON np.id = staff.nucleo_patient_id WHERE np.patient_id = $1 AND np.desvinculado_at IS NULL ORDER BY staff.id",
            &[&patient_id],
        ).await.map_err(database_error)?;
        let current_staff = rows
            .iter()
            .map(NucleoPatientStaffEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        expected_nucleos.sort_by_key(|entity| entity.id);
        expected_staff.sort_by_key(|entity| entity.id);
        if current_nucleos != expected_nucleos || current_staff != expected_staff {
            return Err(AcolhimentoException::new(
                ReasonCode::AcolhimentoConcurrentChange,
            ));
        }
        Ok(())
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
