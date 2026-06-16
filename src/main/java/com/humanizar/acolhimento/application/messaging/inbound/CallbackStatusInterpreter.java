package com.humanizar.acolhimento.application.messaging.inbound;

import org.springframework.stereotype.Component;

@Component
public class CallbackStatusInterpreter {

    private static final String PROCESSED = "PROCESSED";

    public boolean isProcessed(String callbackStatus) {
        return PROCESSED.equalsIgnoreCase(callbackStatus);
    }
}
