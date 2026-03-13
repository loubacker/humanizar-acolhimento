package com.humanizar.acolhimento.infrastructure.config.rabbit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.humanizar.acolhimento.application.catalog.QueueCatalog;

@Configuration
public class RabbitQueueConfig {

    private static final int INBOUND_DELIVERY_LIMIT = 3;

    @Bean
    public Queue callbackNucleoRelacionamentoAcolhimentoDlq() {
        return QueueBuilder.durable(QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ)
                .quorum()
                .build();
    }

    @Bean
    public Queue callbackNucleoRelacionamentoAcolhimentoQueue() {
        return QueueBuilder.durable(QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO)
                .quorum()
                .withArgument("x-delivery-limit", INBOUND_DELIVERY_LIMIT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ)
                .build();
    }
}
