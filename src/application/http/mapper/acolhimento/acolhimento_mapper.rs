use std::collections::HashMap;
use uuid::Uuid;

use crate::application::http::dto::{
    AcolhimentoRetrieveDto, NucleoPatientRetrieveDto, NucleoStaffDto,
};
use crate::domain::model::{Acolhimento, NucleoPatient, NucleoPatientStaff};

pub struct AcolhimentoMapper;

impl AcolhimentoMapper {
    pub fn to_dto(
        patient_id: Uuid,
        acolhimento: Option<&Acolhimento>,
        nucleos: &[NucleoPatient],
        staff_by_nucleo_patient: &HashMap<Uuid, Vec<NucleoPatientStaff>>,
    ) -> AcolhimentoRetrieveDto {
        let (created_at, updated_at) = match acolhimento {
            Some(a) => (Some(a.created_at()), Some(a.updated_at())),
            None => {
                let earliest = nucleos.iter().filter_map(|n| n.created_at()).min();
                let latest = nucleos.iter().filter_map(|n| n.updated_at()).max();
                (earliest, latest)
            }
        };

        let nucleo_dtos = nucleos
            .iter()
            .map(|nucleo| {
                let staff_list = staff_by_nucleo_patient
                    .get(&nucleo.id())
                    .map(|items| {
                        items
                            .iter()
                            .map(|s| NucleoStaffDto {
                                id: Some(s.id()),
                                staff_user_id: Some(s.staff_user_id()),
                                role: Some(s.role().to_owned()),
                            })
                            .collect()
                    })
                    .unwrap_or_default();

                NucleoPatientRetrieveDto {
                    id: nucleo.id(),
                    nucleo_id: nucleo.nucleo_id(),
                    nucleo_patient_staff: staff_list,
                    anamnese: nucleo.anamnese().cloned(),
                }
            })
            .collect();

        AcolhimentoRetrieveDto {
            patient_id,
            created_at,
            updated_at,
            nucleos: nucleo_dtos,
        }
    }
}
