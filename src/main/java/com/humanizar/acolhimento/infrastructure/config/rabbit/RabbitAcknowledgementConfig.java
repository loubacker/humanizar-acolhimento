package com.humanizar.acolhimento.infrastructure.config.rabbit;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import com.rabbitmq.client.Channel;

@Configuration
public class RabbitAcknowledgementConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitAcknowledgementConfig.class);

    public void ack(Channel channel, long deliveryTag, String context) throws IOException {
        try {
            channel.basicAck(deliveryTag, false);
            log.info("RABBIT_ACK|type=ACK|deliveryTag={}|context={}", deliveryTag, context);
        } catch (IOException ex) {
            log.error("Falha ao executar ACK. deliveryTag={}, context={}", deliveryTag, context, ex);
            throw ex;
        }
    }

    public void nackRetry(Channel channel, long deliveryTag, String context) throws IOException {
        try {
            channel.basicNack(deliveryTag, false, true);
            log.warn("RABBIT_ACK|type=NACK_RETRY|deliveryTag={}|context={}", deliveryTag, context);
        } catch (IOException ex) {
            log.error("Falha ao executar NACK_RETRY. deliveryTag={}, context={}", deliveryTag, context, ex);
            throw ex;
        }
    }

    public void nackDeadLetter(Channel channel, long deliveryTag, String context) throws IOException {
        try {
            channel.basicNack(deliveryTag, false, false);
            log.warn("RABBIT_ACK|type=NACK_DLQ|deliveryTag={}|context={}", deliveryTag, context);
        } catch (IOException ex) {
            log.error("Falha ao executar NACK_DLQ. deliveryTag={}, context={}", deliveryTag, context, ex);
            throw ex;
        }
    }
}
