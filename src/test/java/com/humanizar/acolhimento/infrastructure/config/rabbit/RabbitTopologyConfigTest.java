package com.humanizar.acolhimento.infrastructure.config.rabbit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.humanizar.acolhimento.application.catalog.ExchangeCatalog;
import com.humanizar.acolhimento.application.catalog.QueueCatalog;
import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import com.humanizar.acolhimento.infrastructure.config.rabbit.binding.RabbitNucleoRelacionamentoCallbackBindingConfig;
import com.humanizar.acolhimento.infrastructure.config.rabbit.binding.RabbitProgramaCallbackBindingConfig;

@SpringBootTest(classes = {
        RabbitExchangeConfig.class,
        RabbitQueueConfig.class,
        RabbitNucleoRelacionamentoCallbackBindingConfig.class,
        RabbitProgramaCallbackBindingConfig.class
})
class RabbitTopologyConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TopicExchange acolhimentoCommandExchange;

    @Autowired
    private TopicExchange acolhimentoEventExchange;

    @Autowired
    private Queue callbackNucleoRelacionamentoAcolhimentoQueue;

    @Autowired
    private Queue callbackNucleoRelacionamentoAcolhimentoDlq;

    @Autowired
    private Queue callbackProgramaAcolhimentoQueue;

    @Autowired
    private Queue callbackProgramaAcolhimentoDlq;

    @Autowired
    private Binding bindNucleoRelacionamentoProcessed;

    @Autowired
    private Binding bindNucleoRelacionamentoRejected;

    @Autowired
    private Binding bindProgramaProcessed;

    @Autowired
    private Binding bindProgramaRejected;

    @Test
    void shouldDeclareExpectedTopicExchanges() {
        assertEquals(ExchangeCatalog.ACOLHIMENTO_COMMAND, acolhimentoCommandExchange.getName());
        assertEquals(ExchangeCatalog.ACOLHIMENTO_EVENT, acolhimentoEventExchange.getName());
    }

    @Test
    void shouldDeclareExpectedQueuesAndDlqStrategy() {
        assertEquals(QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO,
                callbackNucleoRelacionamentoAcolhimentoQueue.getName());
        assertEquals(QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ,
                callbackNucleoRelacionamentoAcolhimentoDlq.getName());
        assertEquals(QueueCatalog.CALLBACK_ACOLHIMENTO_PROGRAMA,
                callbackProgramaAcolhimentoQueue.getName());
        assertEquals(QueueCatalog.CALLBACK_ACOLHIMENTO_PROGRAMA_DLQ,
                callbackProgramaAcolhimentoDlq.getName());

        assertQueueHasDeliveryLimitAndDlq(
                callbackNucleoRelacionamentoAcolhimentoQueue,
                QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ);
        assertQueueHasDeliveryLimitAndDlq(
                callbackProgramaAcolhimentoQueue,
                QueueCatalog.CALLBACK_ACOLHIMENTO_PROGRAMA_DLQ);
    }

    @Test
    void shouldDeclareExpectedBindings() {
        assertBinding(bindNucleoRelacionamentoProcessed,
                QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO,
                ExchangeCatalog.ACOLHIMENTO_EVENT,
                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1);
        assertBinding(bindNucleoRelacionamentoRejected, QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO,
                ExchangeCatalog.ACOLHIMENTO_EVENT,
                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1);
        assertBinding(bindProgramaProcessed,
                QueueCatalog.CALLBACK_ACOLHIMENTO_PROGRAMA,
                ExchangeCatalog.ACOLHIMENTO_EVENT,
                RoutingKeyCatalog.ACOLHIMENTO_PROGRAMA_PROCESSED_V1);
        assertBinding(bindProgramaRejected,
                QueueCatalog.CALLBACK_ACOLHIMENTO_PROGRAMA,
                ExchangeCatalog.ACOLHIMENTO_EVENT,
                RoutingKeyCatalog.ACOLHIMENTO_PROGRAMA_REJECTED_V1);

        Map<String, Binding> bindings = applicationContext.getBeansOfType(Binding.class);
        assertNotNull(bindings);
        assertEquals(4, bindings.size());
    }

    private void assertQueueHasDeliveryLimitAndDlq(Queue queue, String expectedDlqRoutingKey) {
        Map<String, Object> arguments = queue.getArguments();
        assertNotNull(arguments);
        assertEquals(3, arguments.get("x-delivery-limit"));
        assertEquals("", arguments.get("x-dead-letter-exchange"));
        assertEquals(expectedDlqRoutingKey, arguments.get("x-dead-letter-routing-key"));
        assertEquals("quorum", arguments.get("x-queue-type"));
    }

    private void assertBinding(Binding binding, String expectedDestination, String expectedExchange,
            String expectedRoutingKey) {
        assertEquals(expectedDestination, binding.getDestination());
        assertEquals(expectedExchange, binding.getExchange());
        assertEquals(expectedRoutingKey, binding.getRoutingKey());
        assertEquals(DestinationType.QUEUE, binding.getDestinationType());
    }
}
