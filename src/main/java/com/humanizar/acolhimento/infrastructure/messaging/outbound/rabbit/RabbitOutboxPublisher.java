package com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
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

        CorrelationData correlationData = new CorrelationData(correlationIdFor(event));
        boolean confirmed = Boolean.TRUE.equals(rabbitTemplate.invoke(operations -> {
            operations.send(event.getExchangeName(), event.getRoutingKey(), message, correlationData);
            return operations.waitForConfirms(CONFIRM_TIMEOUT_MS);
        }));

        ReturnedMessage returned = correlationData.getReturned();
        if (returned != null) {
            throw unroutableException(event, returned);
        }

        if (!confirmed) {
            throw new AmqpException("Broker NACK para eventId=" + event.getEventId());
        }

        log.info("Evento publicado e confirmado pelo broker. eventId={}, exchange={}, routingKey={}",
                event.getEventId(), event.getExchangeName(), event.getRoutingKey());
    }

    private String correlationIdFor(OutboxEvent event) {
        if (event.getEventId() != null) {
            return event.getEventId().toString();
        }
        if (event.getCorrelationId() != null) {
            return event.getCorrelationId().toString();
        }
        return "missing-correlation";
    }

    private AmqpException unroutableException(OutboxEvent event, ReturnedMessage returned) {
        return new AmqpException("Mensagem RabbitMQ nao roteavel. eventId=" + event.getEventId()
                + ", exchange=" + returned.getExchange()
                + ", routingKey=" + returned.getRoutingKey()
                + ", replyCode=" + returned.getReplyCode()
                + ", replyText=" + returned.getReplyText());
    }
}
