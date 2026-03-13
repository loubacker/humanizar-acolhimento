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
public class RabbitNucleoRelacionamentoCallbackBindingConfig {

    @Bean
    public Binding bindNucleoRelacionamentoProcessed(
            @Qualifier("callbackNucleoRelacionamentoAcolhimentoQueue") Queue callbackNucleoRelacionamentoAcolhimentoQueue,
            @Qualifier("acolhimentoEventExchange") TopicExchange acolhimentoEventExchange) {
        return BindingBuilder.bind(callbackNucleoRelacionamentoAcolhimentoQueue)
                .to(acolhimentoEventExchange)
                .with(RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1);
    }

    @Bean
    public Binding bindNucleoRelacionamentoRejected(
            @Qualifier("callbackNucleoRelacionamentoAcolhimentoQueue") Queue callbackNucleoRelacionamentoAcolhimentoQueue,
            @Qualifier("acolhimentoEventExchange") TopicExchange acolhimentoEventExchange) {
        return BindingBuilder.bind(callbackNucleoRelacionamentoAcolhimentoQueue)
                .to(acolhimentoEventExchange)
                .with(RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1);
    }
}
