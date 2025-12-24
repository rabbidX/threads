package me.garyanov.threads.ratelimiting;

import lombok.AllArgsConstructor;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


@AllArgsConstructor
public class AdaptiveProducer implements Runnable {
    private final String id;
    private final BlockingQueue<WorkItem> queue;
    private final DynamicRateLimiter rateLimiter;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Random random = new Random();

    @Override
    public void run() {
        while (running.get()) {
            if (rateLimiter.tryAcquire()) {
                WorkItem item = createWorkItem();
                boolean offered = queue.offer(item);
                if (!offered) {
                    // Handle queue full scenario - backpressure
                    handleBackpressure(item);
                } else {
                    System.out.println(Arrays.toString(item.getPayload()) + " is offered");
                }
            }

            // Control production rate with jitter for realism
            try {
                Thread.sleep(calculateNextInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private WorkItem createWorkItem() {
        return new WorkItem(UUID.randomUUID().toString(), Instant.now(), id, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
    }

    private void handleBackpressure(WorkItem item) {
    }

    private long calculateNextInterval() {
        double rate = rateLimiter.getCurrentRate();
        double intervalMs = 1000.0 / rate;
        // Add ±20% jitter to simulate real-world variability
        double jitter = 0.2 * intervalMs * (random.nextDouble() * 2 - 1);
        return Math.max(1, (long) (intervalMs + jitter));
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}