package com.humanizar.acolhimento.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.AcolhimentoDeleteDTO;
import com.humanizar.acolhimento.application.inbound.dto.acolhimento.InboundAcolhimentoDTO;
import com.humanizar.acolhimento.application.inbound.dto.envelop.InboundEnvelopeDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoPatientDTO;
import com.humanizar.acolhimento.application.inbound.dto.nucleo.NucleoResponsavelDTO;
import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDTO;
import com.humanizar.acolhimento.application.outbound.dto.AcolhimentoCommandDeletedDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundEnvelopeDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoPatientDTO;
import com.humanizar.acolhimento.application.outbound.dto.OutboundNucleoResponsavelDTO;

@Configuration
@ImportRuntimeHints(ObjectMapperConfig.ObjectMapperRuntimeHints.class)
public class ObjectMapperConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static class ObjectMapperRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            registerJsonBinding(hints, InboundAcolhimentoDTO.class);
            registerJsonBinding(hints, InboundEnvelopeDTO.class);
            registerJsonBinding(hints, AcolhimentoDeleteDTO.class);
            registerJsonBinding(hints, NucleoPatientDTO.class);
            registerJsonBinding(hints, NucleoResponsavelDTO.class);
            registerJsonBinding(hints, CallbackDTO.class);
            registerJsonBinding(hints, OutboundEnvelopeDTO.class);
            registerJsonBinding(hints, AcolhimentoCommandDTO.class);
            registerJsonBinding(hints, AcolhimentoCommandDeletedDTO.class);
            registerJsonBinding(hints, OutboundNucleoPatientDTO.class);
            registerJsonBinding(hints, OutboundNucleoResponsavelDTO.class);
        }

        private void registerJsonBinding(RuntimeHints hints, Class<?> type) {
            hints.reflection().registerType(type, MemberCategory.values());
        }
    }
}
