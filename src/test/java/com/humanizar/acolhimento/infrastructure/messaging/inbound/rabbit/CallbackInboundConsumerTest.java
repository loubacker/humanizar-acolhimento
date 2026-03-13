package com.humanizar.acolhimento.infrastructure.messaging.inbound.rabbit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoCallbackService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.config.rabbit.RabbitAcknowledgementConfig;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class CallbackInboundConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AcolhimentoCallbackService acolhimentoCallbackService;

    @Mock
    private Channel channel;

    @Test
    void shouldAckOnSuccess() throws IOException {
        long deliveryTag = 11L;
        CallbackDTO callback = callback("PROCESSED");
        when(objectMapper.readValue(any(byte[].class), org.mockito.ArgumentMatchers.eq(CallbackDTO.class)))
                .thenReturn(callback);

        CallbackInboundConsumer consumer = new CallbackInboundConsumer(
                objectMapper,
                acolhimentoCallbackService,
                new RabbitAcknowledgementConfig());

        consumer.onNucleoRelacionamentoCallback(message(deliveryTag), channel);

        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(deliveryTag, false, true);
        verify(channel, never()).basicNack(deliveryTag, false, false);
    }

    @Test
    void shouldAckOnDuplicateEvent() throws IOException {
        long deliveryTag = 12L;
        CallbackDTO callback = callback("PROCESSED");
        when(objectMapper.readValue(any(byte[].class), org.mockito.ArgumentMatchers.eq(CallbackDTO.class)))
                .thenReturn(callback);
        doThrow(new AcolhimentoException(
                        ReasonCode.DUPLICATE_EVENT,
                        callback.correlationId() != null ? callback.correlationId().toString() : null,
                        "duplicado"))
                .when(acolhimentoCallbackService)
                .processCallback(any(), any(), any());

        CallbackInboundConsumer consumer = new CallbackInboundConsumer(
                objectMapper,
                acolhimentoCallbackService,
                new RabbitAcknowledgementConfig());

        consumer.onNucleoRelacionamentoCallback(message(deliveryTag), channel);

        verify(channel).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(deliveryTag, false, true);
        verify(channel, never()).basicNack(deliveryTag, false, false);
    }

    @Test
    void shouldNackRetryOnRetryableException() throws IOException {
        long deliveryTag = 13L;
        CallbackDTO callback = callback("PROCESSED");
        when(objectMapper.readValue(any(byte[].class), org.mockito.ArgumentMatchers.eq(CallbackDTO.class)))
                .thenReturn(callback);
        doThrow(new AcolhimentoException(
                        ReasonCode.PERSISTENCE_FAILURE,
                        callback.correlationId() != null ? callback.correlationId().toString() : null,
                        "falha temporaria"))
                .when(acolhimentoCallbackService)
                .processCallback(any(), any(), any());

        CallbackInboundConsumer consumer = new CallbackInboundConsumer(
                objectMapper,
                acolhimentoCallbackService,
                new RabbitAcknowledgementConfig());

        consumer.onNucleoRelacionamentoCallback(message(deliveryTag), channel);

        verify(channel).basicNack(deliveryTag, false, true);
        verify(channel, never()).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(deliveryTag, false, false);
    }

    @Test
    void shouldNackDeadLetterOnNonRetryableException() throws IOException {
        long deliveryTag = 14L;
        CallbackDTO callback = callback("REJECTED");
        when(objectMapper.readValue(any(byte[].class), org.mockito.ArgumentMatchers.eq(CallbackDTO.class)))
                .thenReturn(callback);
        doThrow(new AcolhimentoException(
                        ReasonCode.VALIDATION_ERROR,
                        callback.correlationId() != null ? callback.correlationId().toString() : null,
                        "falha definitiva"))
                .when(acolhimentoCallbackService)
                .processCallback(any(), any(), any());

        CallbackInboundConsumer consumer = new CallbackInboundConsumer(
                objectMapper,
                acolhimentoCallbackService,
                new RabbitAcknowledgementConfig());

        consumer.onNucleoRelacionamentoCallback(message(deliveryTag), channel);

        verify(channel).basicNack(deliveryTag, false, false);
        verify(channel, never()).basicAck(deliveryTag, false);
        verify(channel, never()).basicNack(deliveryTag, false, true);
    }

    @Test
    void shouldNackDeadLetterOnParseError() throws IOException {
        long deliveryTag = 15L;
        when(objectMapper.readValue(any(byte[].class), org.mockito.ArgumentMatchers.eq(CallbackDTO.class)))
                .thenThrow(new IOException("invalid json"));

        CallbackInboundConsumer consumer = new CallbackInboundConsumer(
                objectMapper,
                acolhimentoCallbackService,
                new RabbitAcknowledgementConfig());

        consumer.onNucleoRelacionamentoCallback(message(deliveryTag), channel);

        verify(channel).basicNack(deliveryTag, false, false);
        verify(acolhimentoCallbackService, never()).processCallback(any(), any(), any());
        verify(channel, never()).basicAck(deliveryTag, false);
    }

    private Message message(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        properties.setConsumerQueue("callback.acolhimento.nucleo-relacionamento");
        properties.setReceivedRoutingKey("ev.acolhimento.nucleo-relacionamento.processed.v1");
        properties.setMessageId(UUID.randomUUID().toString());
        return new Message("{}".getBytes(), properties);
    }

    private CallbackDTO callback(String status) {
        return new CallbackDTO(
                "cmd.acolhimento.created.v1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "humanizar-nucleo-relacionamento",
                "humanizar.acolhimento.event",
                "ev.acolhimento.nucleo-relacionamento.processed.v1",
                "acolhimento",
                UUID.randomUUID(),
                1,
                LocalDateTime.now(),
                UUID.randomUUID(),
                "JUnit",
                "127.0.0.1",
                status,
                null,
                null,
                LocalDateTime.now(),
                null);
    }
}
