package me.garyanov.threads.ratelimiting;

import lombok.AllArgsConstructor;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@AllArgsConstructor
public class AdaptiveConsumer implements Runnable {
    private final String id;
    private final BlockingQueue<WorkItem> queue;
    private final DynamicRateLimiter rateLimiter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final Random random = new Random();

    @Override
    public void run() {
        while (running.get()) {
            try {
                WorkItem item = queue.poll(100, TimeUnit.MILLISECONDS);
                if (item != null) {
                    item.setProcessingStartTime(Instant.now());

                    // Simulate variable processing time
                    simulateProcessing(item);

                    item.setProcessingEndTime(Instant.now());
                    processedCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void simulateProcessing(WorkItem item) {
        // Simulate processing time with variability
        int processingTime = 50 + random.nextInt(100); // 50-150ms
        try {
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running.set(false);
    }
}