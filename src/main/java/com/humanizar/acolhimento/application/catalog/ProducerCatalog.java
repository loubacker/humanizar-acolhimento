package com.humanizar.acolhimento.application.catalog;

public final class ProducerCatalog {

    public static final String PRODUCER_SERVICE = "humanizar-acolhimento";
    public static final String AGGREGATE_TYPE = "acolhimento";
    public static final short EVENT_VERSION = 1;
    public static final int DEFAULT_MAX_ATTEMPTS = 5;

    private ProducerCatalog() {
    }
}
