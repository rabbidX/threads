package me.garyanov.threads.ratelimiting.producer;

import java.util.concurrent.atomic.AtomicBoolean;

public interface Producer extends Runnable{
    AtomicBoolean running = new AtomicBoolean(true);

    default void stop() {
        running.set(false);
    }

    default boolean isRunning() {
        return running.get();
    }
}
