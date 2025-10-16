package main.java.me.garyanov.threads.racecondition;

import main.java.me.garyanov.threads.racecondition.exception.NoMoreElementsException;
import main.java.me.garyanov.threads.racecondition.queue.NaturalNumbersQueue;

import java.util.stream.IntStream;

public class MultithreadedQueueAdder {
    private final static int THREAD_NUMBER = 10;
    private final NaturalNumbersQueue queue;
    private final int[] sums = new int[THREAD_NUMBER];

    public MultithreadedQueueAdder(NaturalNumbersQueue queue) {
        this.queue = queue;
    }

    public int getSum() {
        Thread[] threads = new Thread[THREAD_NUMBER];
        for (int i = 0; i < THREAD_NUMBER; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> setThreadsSum(threadIndex));
        }
        for (Thread thread: threads) {
            thread.start();
        }
        for (Thread thread: threads) {
            try {
                thread.join(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return IntStream.of(sums).sum();
    }

    private void setThreadsSum(int i) {
        while (true) {
            try {
                sums[i] += queue.getNext();
            } catch (NoMoreElementsException e) {
                break;
            }
        }
    }

}
