package com.humanizar.acolhimento.infrastructure.messaging.outbound.rabbit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.humanizar.acolhimento.domain.model.OutboxEvent;

@ExtendWith(MockitoExtension.class)
class RabbitOutboxPublisherTest {

    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EXCHANGE = "humanizar.test.exchange";
    private static final String ROUTING_KEY = "test.routing";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RabbitOperations rabbitOperations;

    private RabbitOutboxPublisher rabbitOutboxPublisher;

    @BeforeEach
    void setUp() {
        rabbitOutboxPublisher = new RabbitOutboxPublisher(rabbitTemplate);
    }

    @Test
    void shouldPublishWhenBrokerConfirmsAndMessageIsRoutable() {
        prepareTemplateInvocation();
        when(rabbitOperations.waitForConfirms(anyLong())).thenReturn(true);

        OutboxEvent event = event();

        assertDoesNotThrow(() -> rabbitOutboxPublisher.publish(event));

        ArgumentCaptor<CorrelationData> correlationCaptor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitOperations).send(eq(EXCHANGE), eq(ROUTING_KEY), any(Message.class), correlationCaptor.capture());
        assertEquals(EVENT_ID.toString(), correlationCaptor.getValue().getId());
    }

    @Test
    void shouldFailWhenBrokerDoesNotConfirm() {
        prepareTemplateInvocation();
        when(rabbitOperations.waitForConfirms(anyLong())).thenReturn(false);

        AmqpException exception = assertThrows(
                AmqpException.class,
                () -> rabbitOutboxPublisher.publish(event()));

        assertTrue(exception.getMessage().contains("Broker NACK"));
        assertTrue(exception.getMessage().contains(EVENT_ID.toString()));
    }

    @Test
    void shouldFailWhenBrokerReturnsUnroutableMessage() {
        prepareTemplateInvocation();
        when(rabbitOperations.waitForConfirms(anyLong())).thenReturn(true);
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3, CorrelationData.class);
            correlationData.setReturned(returnedMessage());
            return null;
        }).when(rabbitOperations).send(eq(EXCHANGE), eq(ROUTING_KEY), any(Message.class), any(CorrelationData.class));

        AmqpException exception = assertThrows(
                AmqpException.class,
                () -> rabbitOutboxPublisher.publish(event()));

        assertTrue(exception.getMessage().contains(EVENT_ID.toString()));
        assertTrue(exception.getMessage().contains(EXCHANGE));
        assertTrue(exception.getMessage().contains(ROUTING_KEY));
        assertTrue(exception.getMessage().contains("312"));
        assertTrue(exception.getMessage().contains("NO_ROUTE"));
    }

    private void prepareTemplateInvocation() {
        when(rabbitTemplate.invoke(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RabbitOperations.OperationsCallback<Boolean> callback =
                    (RabbitOperations.OperationsCallback<Boolean>) invocation.getArgument(0);
            return callback.doInRabbit(rabbitOperations);
        });
    }

    private OutboxEvent event() {
        return OutboxEvent.builder()
                .eventId(EVENT_ID)
                .correlationId(CORRELATION_ID)
                .exchangeName(EXCHANGE)
                .routingKey(ROUTING_KEY)
                .payload("{\"ok\":true}")
                .build();
    }

    private ReturnedMessage returnedMessage() {
        Message message = MessageBuilder.withBody(new byte[0]).build();
        return new ReturnedMessage(message, 312, "NO_ROUTE", EXCHANGE, ROUTING_KEY);
    }
}
