package com.humanizar.acolhimento.application.catalog;

public final class RoutingKeyCatalog {

    // Routing keys de comandos outbound produzidos por acolhimento.
    public static final String COMMAND_ACOLHIMENTO_CREATED_V1 = "cmd.acolhimento.created.v1";
    public static final String COMMAND_ACOLHIMENTO_UPDATED_V1 = "cmd.acolhimento.updated.v1";
    public static final String COMMAND_ACOLHIMENTO_DELETED_V1 = "cmd.acolhimento.deleted.v1";

    // Routing keys de callback inbound consumidos do nucleo-relacionamento.
    public static final String ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1 = "ev.acolhimento.nucleo-relacionamento.processed.v1";
    public static final String ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1 = "ev.acolhimento.nucleo-relacionamento.rejected.v1";

    // Routing keys de callback inbound consumidos do programa.
    public static final String ACOLHIMENTO_PROGRAMA_PROCESSED_V1 = "ev.acolhimento.programa.processed.v1";
    public static final String ACOLHIMENTO_PROGRAMA_REJECTED_V1 = "ev.acolhimento.programa.rejected.v1";

    public static boolean isAcolhimentoEventOutbound(String routingKey) {
        return COMMAND_ACOLHIMENTO_CREATED_V1.equals(routingKey)
                || COMMAND_ACOLHIMENTO_UPDATED_V1.equals(routingKey)
                || COMMAND_ACOLHIMENTO_DELETED_V1.equals(routingKey);
    }

    public static boolean isAcolhimentoNucleoRelacionamentoCallback(String routingKey) {
        return ACOLHIMENTO_NUCLEO_RELACIONAMENTO_PROCESSED_V1.equals(routingKey)
                || ACOLHIMENTO_NUCLEO_RELACIONAMENTO_REJECTED_V1.equals(routingKey);
    }

    public static boolean isAcolhimentoProgramaCallback(String routingKey) {
        return ACOLHIMENTO_PROGRAMA_PROCESSED_V1.equals(routingKey)
                || ACOLHIMENTO_PROGRAMA_REJECTED_V1.equals(routingKey);
    }

    private RoutingKeyCatalog() {
    }
}
