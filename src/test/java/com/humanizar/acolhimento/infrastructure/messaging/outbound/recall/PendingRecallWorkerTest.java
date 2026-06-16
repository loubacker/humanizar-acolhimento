package com.humanizar.acolhimento.infrastructure.messaging.outbound.recall;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingRecallWorkerTest {

    @Mock
    private PendingRecall pendingRecall;

    @InjectMocks
    private PendingRecallWorker worker;

    @Test
    void shouldInvokeRecallWithBatchSize() {
        worker.recall();

        verify(pendingRecall).recall(50);
    }

    @Test
    void shouldNotPropagateExceptionFromCycle() {
        doThrow(new RuntimeException("falha simulada")).when(pendingRecall).recall(50);

        worker.recall();

        verify(pendingRecall).recall(50);
    }
}
