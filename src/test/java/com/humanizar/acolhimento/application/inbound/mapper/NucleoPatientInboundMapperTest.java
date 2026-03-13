package com.humanizar.acolhimento.application.inbound.mapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatient;
import com.humanizar.acolhimento.domain.model.nucleo.NucleoPatientResponsavel;

class NucleoPatientInboundMapperTest {

    private final NucleoPatientInboundMapper mapper = new NucleoPatientInboundMapper();
    private static final String CORRELATION_ID = "corr-123";

    @Test
    void shouldMapNucleosAndResponsaveisSuccessfully() {
        UUID patientId = UUID.randomUUID();
        NucleoPatientDTO primeiro = nucleo(UUID.randomUUID(), responsavel("COORDENADOR"), responsavel("ADMINISTRADOR"));
        NucleoPatientDTO segundo = nucleo(UUID.randomUUID(), responsavel("COORDENADOR"));
        List<NucleoPatientDTO> dtos = List.of(primeiro, segundo);

        Map<UUID, UUID> ids = mapper.reconcileIdsForCreate(dtos, CORRELATION_ID);
        List<NucleoPatient> nucleos = mapper.toDomainList(dtos, patientId, ids, CORRELATION_ID);
        List<NucleoPatientResponsavel> responsaveis = mapper.toResponsavelList(dtos, ids, CORRELATION_ID);

        assertEquals(2, ids.size());
        assertEquals(2, nucleos.size());
        assertEquals(3, responsaveis.size());
        assertEquals(patientId, nucleos.get(0).getPatientId());
        assertEquals(ids.get(primeiro.nucleoId()), nucleos.get(0).getId());
        assertEquals(ids.get(segundo.nucleoId()), nucleos.get(1).getId());
    }

    @Test
    void shouldReuseExistingIdsAndGenerateOnlyForNewOnUpdate() {
        UUID nucleoIdExistente = UUID.randomUUID();
        UUID nucleoIdNovo = UUID.randomUUID();
        UUID idExistente = UUID.randomUUID();

        List<NucleoPatientDTO> dtos = List.of(
                nucleo(nucleoIdExistente, responsavel("COORDENADOR")),
                nucleo(nucleoIdNovo, responsavel("ADMINISTRADOR")));

        Map<UUID, UUID> reconciled = mapper.reconcileIdsForUpdate(
                dtos,
                Map.of(nucleoIdExistente, idExistente),
                CORRELATION_ID);

        assertEquals(idExistente, reconciled.get(nucleoIdExistente));
        assertTrue(reconciled.containsKey(nucleoIdNovo));
        assertNotEquals(idExistente, reconciled.get(nucleoIdNovo));
    }

    @Test
    void shouldFailWhenRoleIsInvalid() {
        List<NucleoPatientDTO> dtos = List.of(
                nucleo(UUID.randomUUID(), new NucleoResponsavelDTO(UUID.randomUUID(), "coordenador")));

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.toResponsavelList(dtos, mapper.reconcileIdsForCreate(dtos, CORRELATION_ID),
                        CORRELATION_ID));

        assertEquals(ReasonCode.INBOUND_INVALID_ENUM, exception.getReasonCode());
    }

    @Test
    void shouldFailWhenNucleoListIsEmpty() {
        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.reconcileIdsForCreate(List.of(), CORRELATION_ID));

        assertEquals(ReasonCode.INBOUND_EMPTY_COLLECTION, exception.getReasonCode());
    }

    @Test
    void shouldFailWhenResponsavelListIsEmpty() {
        List<NucleoPatientDTO> dtos = List.of(new NucleoPatientDTO(UUID.randomUUID(), List.of()));

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.reconcileIdsForCreate(dtos, CORRELATION_ID));

        assertEquals(ReasonCode.INBOUND_EMPTY_COLLECTION, exception.getReasonCode());
    }

    @Test
    void shouldFailWhenNucleoIdIsDuplicated() {
        UUID duplicatedNucleoId = UUID.randomUUID();
        List<NucleoPatientDTO> dtos = List.of(
                nucleo(duplicatedNucleoId, responsavel("COORDENADOR")),
                nucleo(duplicatedNucleoId, responsavel("ADMINISTRADOR")));

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.reconcileIdsForCreate(dtos, CORRELATION_ID));

        assertEquals(ReasonCode.INBOUND_DUPLICATE_ITEM, exception.getReasonCode());
    }

    @Test
    void shouldFailWhenResponsavelIdIsDuplicated() {
        UUID duplicatedResponsavelId = UUID.randomUUID();
        List<NucleoPatientDTO> dtos = List.of(
                nucleo(
                        UUID.randomUUID(),
                        new NucleoResponsavelDTO(duplicatedResponsavelId, "COORDENADOR"),
                        new NucleoResponsavelDTO(duplicatedResponsavelId, "ADMINISTRADOR")));

        AcolhimentoException exception = assertThrows(
                AcolhimentoException.class,
                () -> mapper.reconcileIdsForCreate(dtos, CORRELATION_ID));

        assertEquals(ReasonCode.INBOUND_DUPLICATE_ITEM, exception.getReasonCode());
    }

    private NucleoPatientDTO nucleo(UUID nucleoId, NucleoResponsavelDTO... responsaveis) {
        return new NucleoPatientDTO(nucleoId, List.of(responsaveis));
    }

    private NucleoResponsavelDTO responsavel(String role) {
        return new NucleoResponsavelDTO(UUID.randomUUID(), role);
    }
}
