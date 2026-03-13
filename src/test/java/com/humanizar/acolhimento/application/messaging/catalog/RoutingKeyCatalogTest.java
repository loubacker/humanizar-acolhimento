package com.humanizar.acolhimento.application.messaging.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.humanizar.acolhimento.application.catalog.RoutingKeyCatalog;
import org.junit.jupiter.api.Test;

class RoutingKeyCatalogTest {

        @Test
        void shouldExposeExpectedCommandLiterals() {
                assertEquals("cmd.acolhimento.created.v1", RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1);
                assertEquals("cmd.acolhimento.updated.v1", RoutingKeyCatalog.COMMAND_ACOLHIMENTO_UPDATED_V1);
                assertEquals("cmd.acolhimento.deleted.v1", RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1);
        }

        @Test
        void shouldExposeExpectedCallbackLiterals() {
                assertEquals(
                                "ev.acolhimento.nucleo-relacionamento.processed.v1",
                                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1);
                assertEquals(
                                "ev.acolhimento.nucleo-relacionamento.rejected.v1",
                                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1);
        }

        @Test
        void shouldClassifyAcolhimentoEventOutboundKeys() {
                assertTrue(RoutingKeyCatalog
                                .isAcolhimentoEventOutbound(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1));
                assertTrue(RoutingKeyCatalog
                                .isAcolhimentoEventOutbound(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_UPDATED_V1));
                assertTrue(RoutingKeyCatalog
                                .isAcolhimentoEventOutbound(RoutingKeyCatalog.COMMAND_ACOLHIMENTO_DELETED_V1));
                assertFalse(RoutingKeyCatalog
                                .isAcolhimentoEventOutbound(
                                                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1));
        }

        @Test
        void shouldClassifyNucleoRelacionamentoCallbackKeys() {
                assertTrue(RoutingKeyCatalog.isAcolhimentoNucleoRelacionamentoCallback(
                                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1));
                assertTrue(RoutingKeyCatalog.isAcolhimentoNucleoRelacionamentoCallback(
                                RoutingKeyCatalog.ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1));
                assertFalse(RoutingKeyCatalog
                                .isAcolhimentoNucleoRelacionamentoCallback(
                                                RoutingKeyCatalog.COMMAND_ACOLHIMENTO_CREATED_V1));
        }
}
