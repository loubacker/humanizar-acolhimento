package com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.humanizar.acolhimento.domain.model.OutboxEvent;

@Component
public class RabbitOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitOutboxPublisher.class);
    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final RabbitTemplate rabbitTemplate;

    public RabbitOutboxPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(OutboxEvent event) {
        Message message = MessageBuilder
                .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setCorrelationId(event.getCorrelationId() != null
                        ? event.getCorrelationId().toString()
                        : null)
                .setMessageId(event.getEventId() != null
                        ? event.getEventId().toString()
                        : null)
                .build();

        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(operations -> {
            operations.send(event.getExchangeName(), event.getRoutingKey(), message);
            return operations.waitForConfirms(CONFIRM_TIMEOUT_MS);
        }));

        if (!confirmed) {
            throw new AmqpException("Broker NACK para eventId=" + event.getEventId());
        }

        log.info("Evento publicado e confirmado pelo broker. eventId={}, exchange={}, routingKey={}",
                event.getEventId(), event.getExchangeName(), event.getRoutingKey());
    }
}
