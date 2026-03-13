package com.humanizar.acolhimento.infrastructure.messaging.outbound.outbox;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class OutboxRetryPolicy {

    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_DELAY = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    public LocalDateTime nextRetryAt(int currentAttemptCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << currentAttemptCount);
        long cappedSeconds = Math.min(delaySeconds, MAX_DELAY.toSeconds());
        return LocalDateTime.now().plusSeconds(cappedSeconds);
    }

    public int getDefaultMaxAttempts() {
        return DEFAULT_MAX_ATTEMPTS;
    }

    public boolean isExhausted(int attemptCount, int maxAttempts) {
        return attemptCount >= maxAttempts;
    }
}
