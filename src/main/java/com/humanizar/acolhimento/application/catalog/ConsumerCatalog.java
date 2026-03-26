package com.humanizar.acolhimento.application.catalog;

public final class ConsumerCatalog {

    // Consumer inbound de callback para eventos do nucleo-relacionamento.
    public static final String CALLBACK_NUCLEO_RELACIONAMENTO_CONSUMER = "callback.nucleo-relacionamento.consumer";

    // Consumer inbound de callback para eventos do programa.
    public static final String CALLBACK_PROGRAMA_CONSUMER = "callback.programa.consumer";

    private ConsumerCatalog() {
    }
}
