package com.humanizar.acolhimento.infrastructure.config.rabbit.binding;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;

@Configuration
public class RabbitProgramaCallbackBindingConfig {

    @Bean
    public Binding bindProgramaProcessed(
            @Qualifier("callbackProgramaAcolhimentoQueue") Queue callbackProgramaAcolhimentoQueue,
            @Qualifier("acolhimentoEventExchange") TopicExchange acolhimentoEventExchange) {
        return BindingBuilder.bind(callbackProgramaAcolhimentoQueue)
                .to(acolhimentoEventExchange)
                .with(RoutingKeyCatalog.ACOLHIMENTO_PROGRAMA_PROCESSED_V1);
    }

    @Bean
    public Binding bindProgramaRejected(
            @Qualifier("callbackProgramaAcolhimentoQueue") Queue callbackProgramaAcolhimentoQueue,
            @Qualifier("acolhimentoEventExchange") TopicExchange acolhimentoEventExchange) {
        return BindingBuilder.bind(callbackProgramaAcolhimentoQueue)
                .to(acolhimentoEventExchange)
                .with(RoutingKeyCatalog.ACOLHIMENTO_PROGRAMA_REJECTED_V1);
    }
}
