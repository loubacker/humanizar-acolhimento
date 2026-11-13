use crate::application::http::dto::{
    NucleoPatientHistoryDetailDto, NucleoPatientHistoryDto, NucleoPatientHistoryPageDto,
    NucleoPatientHistoryStaffDto,
};
use crate::domain::model::{NucleoPatientHistory, NucleoPatientHistoryPage};

pub struct AcolhimentoHistoryMapper;

impl AcolhimentoHistoryMapper {
    pub fn item(value: NucleoPatientHistory) -> NucleoPatientHistoryDto {
        NucleoPatientHistoryDto {
            nucleo_patient_id: value.id,
            nucleo_id: value.nucleo_id,
            created_at: value.created_at,
            desvinculado_at: value.desvinculado_at,
            desvinculado_por: value.desvinculado_por,
            desvinculo_operation_id: value.desvinculo_operation_id,
            possui_anamnese: value.possui_anamnese,
            responsaveis: value
                .responsaveis
                .into_iter()
                .map(|staff| NucleoPatientHistoryStaffDto {
                    id: staff.id(),
                    staff_user_id: staff.staff_user_id(),
                    role: staff.role().to_owned(),
                })
                .collect(),
        }
    }

    pub fn page(value: NucleoPatientHistoryPage) -> NucleoPatientHistoryPageDto {
        NucleoPatientHistoryPageDto {
            content: value.content.into_iter().map(Self::item).collect(),
            page: value.page,
            size: value.size,
            total_elements: value.total_elements,
            total_pages: value.total_elements.div_ceil(u64::from(value.size)),
        }
    }

    pub fn detail(mut value: NucleoPatientHistory) -> NucleoPatientHistoryDetailDto {
        let anamnese = value.anamnese.take();
        NucleoPatientHistoryDetailDto {
            periodo: Self::item(value),
            anamnese,
        }
    }
}
