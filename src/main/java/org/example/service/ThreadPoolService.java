package org.example.service;

import java.util.LinkedList;
import java.util.Queue;

public class ThreadPoolService {

    private final Queue<Runnable> queue = new LinkedList<>();

    public ThreadPoolService(int noOfThreads) {
        for (int i =0 ;i<noOfThreads ;i++) {
            TaskExecutor taskExecutor = new TaskExecutor(queue);
            new Thread(taskExecutor).start();
        }
    }

    public void submit(Runnable runnable){
        queue.add(runnable);
    }

    private class TaskExecutor implements Runnable {

        private final Queue<Runnable> queue;

        public TaskExecutor(Queue<Runnable> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true){
                Runnable runnable = queue.poll();
                if(runnable != null){
                    runnable.run();
                }
            }
        }
    }
}
