package me.garyanov.threads.ratelimiting.model;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class LastProcessedItemsCollection {
    private final ConcurrentLinkedQueue<WorkItem> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantLock cleanupLock = new ReentrantLock();
    private final ReentrantLock recalculateStaLock = new ReentrantLock();
    private final AtomicInteger itemsLastSecond = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private volatile Instant lastStatsUpdate = Instant.now();

    public void add(WorkItem item) {
        if (item.getProcessingEndTime() == null) {
            throw new IllegalArgumentException("Processing end time cannot be null");
        }
        queue.offer(item);
        cleanupOldItems(item.getProcessingEndTime());
    }

    public int getCountLastSecond() {
        cleanupOldItems(Instant.now());
        updateStatsIfNeeded();
        return itemsLastSecond.get();
    }

    public double getAverageProcessingTimeMillis() {
        cleanupOldItems(Instant.now());
        updateStatsIfNeeded();
        if (itemsLastSecond.get() == 0) {
            return 0.0;
        }
        return (double) totalProcessingTime.get() / itemsLastSecond.get();
    }

    private void cleanupOldItems(Instant referenceTime ) {
        var cutoffTime = referenceTime.minusSeconds(1);
        if (cleanupLock.tryLock()) {
            try {
                queue.removeIf(workItem -> workItem.getProcessingEndTime().isBefore(cutoffTime));
            } finally {
                cleanupLock.unlock();
            }
        }
    }

    private void updateStatsIfNeeded() {
        Instant now = Instant.now();
        if (Duration.between(lastStatsUpdate, now).toMillis() > 100) {
            recalculateStat();
            lastStatsUpdate = now;
        }
    }

    private void recalculateStat() {
        if (recalculateStaLock.tryLock()) {
            try {
                itemsLastSecond.set(queue.size());
                totalProcessingTime.set(queue.stream()
                        .map(item -> Duration.between(item.getCreatedTime(), item.getProcessingEndTime()).toMillis())
                        .reduce(Long::sum)
                        .orElse(0L));
            } finally {
                recalculateStaLock.unlock();
            }
        }
    }
}
