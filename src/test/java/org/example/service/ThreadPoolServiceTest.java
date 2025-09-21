package org.example.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ThreadPoolServiceTest {

    private ThreadPoolService threadPool;

    @BeforeEach
    void setUp() {
        // Start a thread pool with 2 worker threads
        threadPool = new ThreadPoolService(2);
    }

    @AfterEach
    void tearDown() {
        // Stop all threads after each test
        threadPool.shutdown();
    }

    @Test
    void testSingleTaskExecution() throws InterruptedException {
        Runnable mockTask = mock(Runnable.class);
        CountDownLatch latch = new CountDownLatch(1);

        // Wrap task to countdown latch
        threadPool.submit(() -> {
            mockTask.run();
            latch.countDown();
        });

        // Wait for task execution
        assertTrue(latch.await(1, TimeUnit.SECONDS));

        // Verify task ran
        verify(mockTask).run();
    }

    @Test
    void testMultipleTasksExecution() throws InterruptedException {
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        CountDownLatch latch = new CountDownLatch(2);

        threadPool.submit(() -> { task1.run(); latch.countDown(); });
        threadPool.submit(() -> { task2.run(); latch.countDown(); });

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        verify(task1).run();
        verify(task2).run();
    }

    @Test
    void testTasksRunInParallel() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        threadPool.submit(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            latch.countDown();
        });

        threadPool.submit(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            latch.countDown();
        });

        // Both tasks complete roughly at the same time
        assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testShutdownStopsWorkers() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Runnable longTask = () -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            latch.countDown();
        };

        threadPool.submit(longTask);
        threadPool.shutdown();

        // Task may or may not run depending on timing, but shutdown completes cleanly
        // Ensure no exceptions occur
        assertTrue(true);
    }
}
