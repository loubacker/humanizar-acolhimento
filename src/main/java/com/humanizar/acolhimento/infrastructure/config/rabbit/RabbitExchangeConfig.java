package com.humanizar.acolhimento.infrastructure.config.rabbit;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;

@Configuration
public class RabbitExchangeConfig {

    @Bean
    public TopicExchange acolhimentoCommandExchange() {
        return new TopicExchange(ExchangeCatalog.ACOLHIMENTO_COMMAND, true, false);
    }

    @Bean
    public TopicExchange acolhimentoEventExchange() {
        return new TopicExchange(ExchangeCatalog.ACOLHIMENTO_EVENT, true, false);
    }
}
