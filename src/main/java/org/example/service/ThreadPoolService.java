package org.example.service;

import java.util.LinkedList;
import java.util.Queue;

public class ThreadPoolService {

    private final Queue<Runnable> queue = new LinkedList<>();
    private final TaskExecutor[] executors;

    public ThreadPoolService(int noOfThreads) {
        executors = new TaskExecutor[noOfThreads];
        for (int i = 0; i < noOfThreads; i++) {
            executors[i] = new TaskExecutor(queue);
            new Thread(executors[i], "ThreadPoolWorker-" + i).start();
        }
    }

    public void submit(Runnable runnable) {
        synchronized (queue) {
            queue.add(runnable);
            queue.notifyAll(); // wake up worker threads
        }
    }

    public void shutdown() {
        for (TaskExecutor executor : executors) {
            executor.stop();
        }
    }

    private static class TaskExecutor implements Runnable {

        private final Queue<Runnable> queue;
        private volatile boolean running = true;

        public TaskExecutor(Queue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (running) {
                Runnable runnable;
                synchronized (queue) {
                    runnable = queue.poll();
                    if (runnable == null) {
                        try {
                            queue.wait();
                        } catch (InterruptedException ignored) {}
                        continue;
                    }
                }
                try {
                    runnable.run();
                } catch (Exception ignored) {}
            }
        }

        public void stop() {
            running = false;
            synchronized (queue) {
                queue.notifyAll();
            }
        }
    }
}
