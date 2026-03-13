package com.humanizar.acolhimento.infrastructure.config.rabbit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.TaskExecutorAdapter;

@Configuration
public class RabbitConnectionConfig {

    private static final Logger logger = Logger.getLogger(RabbitConnectionConfig.class.getName());

    @Bean
    public ConnectionNameStrategy connectionName(
            @Value("${spring.application.name}") String applicationName,
            @Value("${server.port}") int servicePort) {
        String hostIp = detectServiceHostIp();
        String connectionName = buildConnectionName(applicationName, hostIp, servicePort);
        logger.info(String.format("RabbitMQ connection name configurado: %s", connectionName));
        return ignored -> connectionName;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            @Qualifier("inboundExecutor") Executor inboundExecutor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setTaskExecutor(new TaskExecutorAdapter(inboundExecutor));
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    private String buildConnectionName(String applicationName, String hostIp, int servicePort) {
        return String.format("%s@%s:%d", applicationName, hostIp, servicePort);
    }

    private String detectServiceHostIp() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("::1")) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("8.8.8.8", 80));
                    ipAddress = socket.getLocalAddress().getHostAddress();
                } catch (IOException e) {
                    logger.warning("Nao foi possivel determinar IP de rede, usando localhost");
                    ipAddress = "localhost";
                }
            }
            return ipAddress;
        } catch (UnknownHostException e) {
            logger.warning("Falha ao detectar IP local, usando localhost");
            return "localhost";
        }
    }
}
