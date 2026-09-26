package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class LatestTaskExecutorTest {

    @Test
    void latestSelectionWins() throws InterruptedException {
        List<Integer> recorded = new CopyOnWriteArrayList<>();
        CountDownLatch lastTaskDone = new CountDownLatch(1);
        int lastIndex = 9;

        try (LatestTaskExecutor executor = new LatestTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                int index = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    recorded.add(index);
                    if (index == lastIndex) {
                        lastTaskDone.countDown();
                    }
                });
            }

            assertTrue(lastTaskDone.await(5, TimeUnit.SECONDS), "the last submitted task should run to completion");
        }

        assertTrue(recorded.size() < 10, "superseded tasks should have been cancelled: " + recorded);
        assertEquals(lastIndex, recorded.get(recorded.size() - 1));
    }

    @Test
    void closeStopsExecutor() {
        LatestTaskExecutor executor = new LatestTaskExecutor();
        executor.submit(() -> {});
        executor.close();

        assertThrows(RejectedExecutionException.class, () -> executor.submit(() -> {}));
    }
}
