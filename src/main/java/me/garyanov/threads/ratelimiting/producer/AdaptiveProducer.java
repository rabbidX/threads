package me.garyanov.threads.ratelimiting.producer;

import lombok.AllArgsConstructor;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;


@AllArgsConstructor
public class AdaptiveProducer implements Producer {
    private final String id;
    private final BlockingQueue<WorkItem> queue;
    private final BlockingQueue<WorkItem> deadLetterQueue;
    private final DynamicRateLimiter rateLimiter;

    @Override
    public void run() {
        while (isRunning()) {
            if (rateLimiter.tryAcquire()) {
                System.out.println("Successful acquire rate limiter");
                WorkItem item = createWorkItem();
                boolean offered = queue.offer(item);
                if (!offered) {
                    // Handle queue full scenario - backpressure
                    handleBackpressure(item);
                } else {
                    System.out.println(Arrays.toString(item.getPayload()) + " is offered");
                }
            } else {
                System.out.println("Attempt to acquire rate limiter was failed");
            }

            // Control production rate with jitter for realism
            try {
                Thread.sleep(rateLimiter.calculateNextInterval());
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
        deadLetterQueue.add(item);
        System.out.println(Arrays.toString(item.getPayload()) + " add to Dead Letter Queue");
    }

}