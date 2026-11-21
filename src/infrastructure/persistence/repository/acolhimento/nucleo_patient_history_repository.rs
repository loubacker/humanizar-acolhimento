use serde_json::Value;
use uuid::Uuid;

use crate::domain::exception::AcolhimentoException;
use crate::infrastructure::cache::cadastral_cache::{
    CadastralCache, RECORDS_CAPACITY, SEARCH_CAPACITY,
};
use crate::infrastructure::config::DatabaseConfig;
use crate::infrastructure::handler::database_error;
use crate::infrastructure::persistence::entity::{
    NucleoPatientHistoryEntity, NucleoPatientStaffEntity,
};

type HistoryRows = (
    Vec<NucleoPatientHistoryEntity>,
    Vec<NucleoPatientStaffEntity>,
    u64,
);

pub(crate) struct NucleoPatientHistoryRepository {
    database: DatabaseConfig,
    pages: CadastralCache<(Uuid, u32, u32), HistoryRows>,
}

impl NucleoPatientHistoryRepository {
    pub fn new(database: DatabaseConfig) -> Self {
        let pages = CadastralCache::new_scoped(
            database.history_cache_state.clone(),
            SEARCH_CAPACITY,
            |key: &(Uuid, u32, u32)| key.0,
            |_, data| history_weight(data),
        );
        Self {
            database,
            pages,
        }
    }

    pub async fn list(
        &self,
        patient_id: Uuid,
        page: u32,
        size: u32,
    ) -> Result<HistoryRows, AcolhimentoException> {
        self.pages
            .get_or_load(
                (patient_id, page, size),
                self.load_page(patient_id, page, size),
            )
            .await
    }

    async fn load_page(
        &self,
        patient_id: Uuid,
        page: u32,
        size: u32,
    ) -> Result<HistoryRows, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client
            .build_transaction()
            .isolation_level(tokio_postgres::IsolationLevel::RepeatableRead)
            .read_only(true)
            .start()
            .await
            .map_err(database_error)?;
        let total: i64 = transaction
            .query_one(
                r#"
                SELECT count(*)
                FROM nucleo_patient
                WHERE patient_id = $1
                    AND desvinculado_at IS NOT NULL
                "#,
                &[&patient_id],
            )
            .await
            .map_err(database_error)?
            .try_get(0)
            .map_err(database_error)?;
        let offset = i64::from(page) * i64::from(size);
        let rows = transaction
            .query(
                r#"
                SELECT
                    id,
                    nucleo_id,
                    created_at,
                    desvinculado_at,
                    desvinculado_por,
                    desvinculo_operation_id,
                    anamnese IS NOT NULL AS possui_anamnese,
                    NULL::jsonb AS anamnese
                FROM nucleo_patient
                WHERE patient_id = $1
                    AND desvinculado_at IS NOT NULL
                ORDER BY desvinculado_at DESC, id DESC
                LIMIT $2 OFFSET $3
                "#,
                &[&patient_id, &i64::from(size), &offset],
            )
            .await
            .map_err(database_error)?;
        let entries = rows
            .iter()
            .map(NucleoPatientHistoryEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        let staff = Self::staff(&transaction, &entries).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok((
            entries,
            staff,
            u64::try_from(total).map_err(|error| {
                AcolhimentoException::with_source(
                    crate::domain::model::enums::ReasonCode::PersistenceFailure,
                    error,
                )
            })?,
        ))
    }

    pub async fn detail(
        &self,
        patient_id: Uuid,
        id: Uuid,
    ) -> Result<HistoryRows, AcolhimentoException> {
        let mut client = self.database.acquire().await?;
        let transaction = client
            .build_transaction()
            .isolation_level(tokio_postgres::IsolationLevel::RepeatableRead)
            .read_only(true)
            .start()
            .await
            .map_err(database_error)?;
        let rows = transaction
            .query(
                r#"
                SELECT
                    id,
                    nucleo_id,
                    created_at,
                    desvinculado_at,
                    desvinculado_por,
                    desvinculo_operation_id,
                    anamnese IS NOT NULL AS possui_anamnese,
                    anamnese
                FROM nucleo_patient
                WHERE patient_id = $1
                    AND id = $2
                    AND desvinculado_at IS NOT NULL
                "#,
                &[&patient_id, &id],
            )
            .await
            .map_err(database_error)?;
        let entries = rows
            .iter()
            .map(NucleoPatientHistoryEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)?;
        let staff = Self::staff(&transaction, &entries).await?;
        transaction.commit().await.map_err(database_error)?;
        Ok((entries, staff, 0))
    }

    async fn staff(
        transaction: &tokio_postgres::Transaction<'_>,
        entries: &[NucleoPatientHistoryEntity],
    ) -> Result<Vec<NucleoPatientStaffEntity>, AcolhimentoException> {
        if entries.is_empty() {
            return Ok(Vec::new());
        }
        let ids: Vec<Uuid> = entries.iter().map(|entry| entry.id).collect();
        let rows = transaction
            .query(
                r#"
                SELECT id, nucleo_patient_id, staff_user_id, role
                FROM nucleo_patient_staff
                WHERE nucleo_patient_id = ANY($1)
                ORDER BY id
                "#,
                &[&ids],
            )
            .await
            .map_err(database_error)?;
        rows.iter()
            .map(NucleoPatientStaffEntity::from_row)
            .collect::<Result<Vec<_>, _>>()
            .map_err(database_error)
    }
}

fn history_weight((periods, staff, _): &HistoryRows) -> u32 {
    let clinical = periods
        .iter()
        .filter_map(|period| period.anamnese.as_ref())
        .map(json_heap_bytes)
        .sum::<usize>();
    let roles = staff
        .iter()
        .map(|member| member.role.capacity())
        .sum::<usize>();
    let bytes = size_of::<HistoryRows>()
        + periods.capacity() * size_of::<NucleoPatientHistoryEntity>()
        + staff.capacity() * size_of::<NucleoPatientStaffEntity>()
        + clinical
        + roles;
    u32::try_from(bytes).unwrap_or(u32::MAX)
}

fn json_heap_bytes(value: &Value) -> usize {
    match value {
        Value::String(text) => text.capacity(),
        Value::Array(items) => {
            items.capacity() * size_of::<Value>() + items.iter().map(json_heap_bytes).sum::<usize>()
        }
        Value::Object(fields) => fields
            .iter()
            .map(|(key, value)| {
                size_of::<(String, Value)>()
                    + size_of::<usize>() * 4
                    + key.capacity()
                    + json_heap_bytes(value)
            })
            .sum(),
        Value::Null | Value::Bool(_) | Value::Number(_) => 0,
    }
}
