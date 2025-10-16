package main.java.me.garyanov.threads.racecondition;

import main.java.me.garyanov.threads.racecondition.exception.NoMoreElementsException;
import main.java.me.garyanov.threads.racecondition.queue.NaturalNumbersQueue;

import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MultithreadedQueueAdder {
    private final static int THREAD_NUMBER = 10;
    private final NaturalNumbersQueue queue;
    private final int[] sums = new int[THREAD_NUMBER];
    String[] threadNames = Stream.generate(UUID::randomUUID)
            .limit(THREAD_NUMBER)
            .map(UUID::toString)
            .toArray(String[]::new);


    public MultithreadedQueueAdder(NaturalNumbersQueue queue) {
        this.queue = queue;
    }

    public int getSum() {
        Thread[] threads = new Thread[THREAD_NUMBER];
        for (int i = 0; i < THREAD_NUMBER; i++) {
            threads[i] = new Thread(this::setThreadsSum, threadNames[i]);
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

    private void setThreadsSum() {
        while (true) {
            try {
                sums[getThreadNumber()] += queue.getNext();
            } catch (NoMoreElementsException e) {
                break;
            }
        }
    }

    private int getThreadNumber() {
        var threadName = Thread.currentThread().getName();
        for (int i = 0; i < THREAD_NUMBER; i++) {
            if (threadName.equals(threadNames[i])) {
                return i;
            }
        }
        throw new RuntimeException("Cannot define thread number");
    }
}
