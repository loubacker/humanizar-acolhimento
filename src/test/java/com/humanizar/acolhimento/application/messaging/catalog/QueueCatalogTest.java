package com.humanizar.acolhimento.application.messaging.catalog;

import com.humanizar.acolhimento.application.catalog.QueueCatalog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueueCatalogTest {

    @Test
    void shouldExposeExpectedQueueLiterals() {
        Assertions.assertEquals("callback.acolhimento.nucleo-relacionamento",
                QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO);
        Assertions.assertEquals("callback.acolhimento.nucleo-relacionamento.dlq",
                QueueCatalog.CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ);
    }
}
