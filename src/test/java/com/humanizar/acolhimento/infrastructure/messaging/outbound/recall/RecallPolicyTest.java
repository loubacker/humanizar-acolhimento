package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class RecallPolicyTest {

    private final RecallPolicy policy = new RecallPolicy();

    @Test
    void firstRecallAtShouldApplyStaleThreshold() {
        LocalDateTime first = policy.firstRecallAt();

        assertTrue(first.isAfter(LocalDateTime.now().plusMinutes(4)));
        assertTrue(first.isBefore(LocalDateTime.now().plusMinutes(6)));
    }

    @Test
    void nextRecallAtShouldGrowAndCap() {
        LocalDateTime firstAttempt = policy.nextRecallAt(0);
        LocalDateTime laterAttempt = policy.nextRecallAt(3);

        assertTrue(firstAttempt.isBefore(laterAttempt));
        // cap em 30min: mesmo com expoente alto nao deve passar de ~31min
        assertTrue(policy.nextRecallAt(20).isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void isExhaustedShouldHonorMaxAttempts() {
        assertFalse(policy.isExhausted(9));
        assertTrue(policy.isExhausted(10));
        assertTrue(policy.isExhausted(11));
    }
}
