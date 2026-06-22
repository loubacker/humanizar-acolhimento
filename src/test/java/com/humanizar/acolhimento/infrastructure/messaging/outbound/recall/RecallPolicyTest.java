package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

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
        assertTrue(policy.nextRecallAt(20).isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void nextRecallAtShouldRemainCappedForHighAttemptCount() {
        LocalDateTime highAttempt = policy.nextRecallAt(100);

        assertTrue(highAttempt.isAfter(LocalDateTime.now().plusMinutes(29)));
        assertTrue(highAttempt.isBefore(LocalDateTime.now().plusMinutes(31)));
    }
}
