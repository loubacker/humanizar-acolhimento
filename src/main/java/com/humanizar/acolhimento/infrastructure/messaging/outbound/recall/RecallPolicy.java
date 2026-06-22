package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class RecallPolicy {

    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(5);
    private static final Duration BASE_DELAY = Duration.ofSeconds(30);
    private static final Duration MAX_DELAY = Duration.ofMinutes(30);
    private static final int MAX_BACKOFF_SHIFT = 30;

    public LocalDateTime firstRecallAt() {
        return LocalDateTime.now().plus(STALE_THRESHOLD);
    }

    public LocalDateTime nextRecallAt(int currentAttemptCount) {
        int backoffShift = Math.min(currentAttemptCount, MAX_BACKOFF_SHIFT);
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << backoffShift);
        long cappedSeconds = Math.min(delaySeconds, MAX_DELAY.toSeconds());
        return LocalDateTime.now().plusSeconds(cappedSeconds);
    }
}
