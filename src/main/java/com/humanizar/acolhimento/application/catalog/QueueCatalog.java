package com.humanizar.acolhimento.application.catalog;

public final class QueueCatalog {

    // Fila inbound de callback vinda do nucleo-relacionamento.
    public static final String CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO = "callback.acolhimento.nucleo-relacionamento";
    public static final String CALLBACK_ACOLHIMENTO_NUCLEO_RELACIONAMENTO_DLQ = "callback.acolhimento.nucleo-relacionamento.dlq";

    // Fila inbound de callback vinda do programa.
    public static final String CALLBACK_ACOLHIMENTO_PROGRAMA = "callback.acolhimento.programa";
    public static final String CALLBACK_ACOLHIMENTO_PROGRAMA_DLQ = "callback.acolhimento.programa.dlq";

    private QueueCatalog() {

    }
}
