package com.humanizar.acolhimento.application.catalog;

public final class ExchangeCatalog {

    // Exchange command outbound de acolhimento consumida pelos serviços downstream.
    public static final String ACOLHIMENTO_COMMAND = "humanizar.acolhimento.command";

    // Exchange de events inbound de acolhimento para callback.
    public static final String ACOLHIMENTO_EVENT = "humanizar.acolhimento.event";

    private ExchangeCatalog() {
    }
}
