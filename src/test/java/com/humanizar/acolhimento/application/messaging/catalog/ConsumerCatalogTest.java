package com.humanizar.acolhimento.application.messaging.catalog;

import com.humanizar.acolhimento.application.catalog.ConsumerCatalog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConsumerCatalogTest {

    @Test
    void shouldExposeExpectedConsumerLiterals() {
        Assertions.assertEquals(
                "callback.nucleo-relacionamento.consumer",
                ConsumerCatalog.CALLBACK_NUCLEO_RELACIONAMENTO_CONSUMER);
    }
}
