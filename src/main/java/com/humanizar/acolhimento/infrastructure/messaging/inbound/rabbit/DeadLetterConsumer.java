package com.humanizar.acolhimento.infrastructure.messaging.inbound.rabbit;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humanizar.acolhimento.application.catalog.QueueCatalog;
import com.humanizar.acolhimento.infrastructure.config.rabbit.RabbitAcknowledgementConfig;
import com.rabbitmq.client.Channel;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final ObjectMapper objectMapper;
    private final RabbitAcknowledgementConfig rabbitAcknowledgementConfig;

    public DeadLetterConsumer(
            ObjectMapper objectMapper,
            RabbitAcknowledgementConfig rabbitAcknowledgementConfig) {
        this.objectMapper = objectMapper;
        this.rabbitAcknowledgementConfig = rabbitAcknowledgementConfig;
    }

    @RabbitListener(
            queues = QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ,
            containerFactory = "rabbitListenerContainerFactory")
    public void onDeadLetter(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String originalRoutingKey = extractOriginalRoutingKey(message);
        String queue = message.getMessageProperties().getConsumerQueue();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            JsonNode body = objectMapper.readTree(message.getBody());
            String correlationId = asText(body, "correlationId");
            String eventId = asText(body, "eventId");

            log.error(
                    "Mensagem dead-lettered recebida. queue={}, messageId={}, originalRoutingKey={}, eventId={}, correlationId={}",
                    queue,
                    messageId,
                    originalRoutingKey,
                    eventId,
                    correlationId);
        } catch (IOException ex) {
            log.error(
                    "Falha ao parsear mensagem da DLQ. queue={}, messageId={}, originalRoutingKey={}. Mensagem sera confirmada.",
                    queue,
                    messageId,
                    originalRoutingKey,
                    ex);
        }

        String context = "queue=" + queue
                + ",messageId=" + messageId
                + ",originalRoutingKey=" + originalRoutingKey;
        rabbitAcknowledgementConfig.ack(channel, deliveryTag, context);
    }

    private String extractOriginalRoutingKey(Message message) {
        List<Map<String, ?>> xDeath = message.getMessageProperties().getXDeathHeader();
        if (xDeath != null && !xDeath.isEmpty()) {
            Map<String, ?> first = xDeath.getFirst();
            Object routingKeys = first.get("routing-keys");
            if (routingKeys instanceof List<?> keys && !keys.isEmpty()) {
                return keys.getFirst().toString();
            }
        }
        return message.getMessageProperties().getReceivedRoutingKey();
    }

    private String asText(JsonNode body, String fieldName) {
        JsonNode node = body.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }
}
