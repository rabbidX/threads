package me.garyanov.threads.racecondition.queue;

import me.garyanov.threads.racecondition.exception.NoMoreElementsException;

import java.util.stream.IntStream;

public class NaturalNumbersQueue {

    protected final int[] values = IntStream.rangeClosed(1, 50000).toArray();
    protected int current = 0;

    public int getNext() {
        if (current == values.length) {
            throw new NoMoreElementsException();
        }
        return values[current++];
    }

    public int getSum() {
        return IntStream.of(values).sum();
    }
}
