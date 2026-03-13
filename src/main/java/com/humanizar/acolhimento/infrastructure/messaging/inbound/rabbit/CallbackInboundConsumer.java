package com.humanizar.acolhimento.infrastructure.messaging.inbound.rabbit;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.ConsumerCatalog;
import com.humanizar.acolhimento.application.catalog.QueueCatalog;
import com.humanizar.acolhimento.application.outbound.CallbackDTO;
import com.humanizar.acolhimento.application.service.AcolhimentoCallbackService;
import com.humanizar.acolhimento.domain.exception.AcolhimentoException;
import com.humanizar.acolhimento.domain.model.enums.ReasonCode;
import com.humanizar.acolhimento.infrastructure.config.rabbit.RabbitAcknowledgementConfig;
import com.rabbitmq.client.Channel;

@Component
public class CallbackInboundConsumer {

    private static final Logger log = LoggerFactory.getLogger(CallbackInboundConsumer.class);
    private static final String TARGET_NUCLEO_RELACIONAMENTO = "humanizar-nucleo-relacionamento";

    private final ObjectMapper objectMapper;
    private final AcolhimentoCallbackService acolhimentoCallbackService;
    private final RabbitAcknowledgementConfig rabbitAcknowledgementConfig;

    public CallbackInboundConsumer(
            ObjectMapper objectMapper,
            AcolhimentoCallbackService acolhimentoCallbackService,
            RabbitAcknowledgementConfig rabbitAcknowledgementConfig) {
        this.objectMapper = objectMapper;
        this.acolhimentoCallbackService = acolhimentoCallbackService;
        this.rabbitAcknowledgementConfig = rabbitAcknowledgementConfig;
    }

    @RabbitListener(queues = QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO, containerFactory = "rabbitListenerContainerFactory")
    public void onNucleoRelacionamentoCallback(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String queue = message.getMessageProperties().getConsumerQueue();
        String messageId = message.getMessageProperties().getMessageId();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        CallbackDTO callback;
        try {
            callback = objectMapper.readValue(message.getBody(), CallbackDTO.class);
        } catch (IOException ex) {
            rabbitAcknowledgementConfig.nackDeadLetter(
                    channel,
                    deliveryTag,
                    buildContext(queue, messageId, routingKey, null, null));
            return;
        }

        String eventId = callback.eventId() != null ? callback.eventId().toString() : null;
        String correlationId = callback.correlationId() != null ? callback.correlationId().toString() : null;
        String context = buildContext(queue, messageId, routingKey, eventId, correlationId);

        try {
            acolhimentoCallbackService.processCallback(
                    ConsumerCatalog.CALLBACK_NUCLEO_RELACIONAMENTO_CONSUMER,
                    TARGET_NUCLEO_RELACIONAMENTO,
                    callback);
            rabbitAcknowledgementConfig.ack(channel, deliveryTag, context);
        } catch (AcolhimentoException ex) {
            if (ex.getReasonCode() == ReasonCode.DUPLICATE_EVENT) {
                log.info("Callback duplicado confirmado. {}", context);
                rabbitAcknowledgementConfig.ack(channel, deliveryTag, context);
                return;
            }
            if (ex.isRetryable()) {
                rabbitAcknowledgementConfig.nackRetry(channel, deliveryTag, context);
                return;
            }
            rabbitAcknowledgementConfig.nackDeadLetter(channel, deliveryTag, context);
        } catch (RuntimeException ex) {
            rabbitAcknowledgementConfig.nackRetry(channel, deliveryTag, context);
        }
    }

    private String buildContext(
            String queue,
            String messageId,
            String routingKey,
            String eventId,
            String correlationId) {
        return "queue=" + queue
                + ",messageId=" + messageId
                + ",routingKey=" + routingKey
                + ",eventId=" + eventId
                + ",correlationId=" + correlationId;
    }
}
