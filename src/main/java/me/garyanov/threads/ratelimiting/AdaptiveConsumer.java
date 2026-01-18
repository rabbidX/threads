package me.garyanov.threads.ratelimiting;

import lombok.AllArgsConstructor;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.LastProcessedItemsCollection;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.time.Instant;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@AllArgsConstructor
public class AdaptiveConsumer implements Runnable {
    private final String id;
    private final BlockingQueue<WorkItem> queue;
    private final ExecutorService lastItemCollector;
    private final LastProcessedItemsCollection lastItems;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicLong processedCount = new AtomicLong(0);
    private final Random random = new Random();

    @Override
    public void run() {
        while (isRunning()) {
            try {
                WorkItem item = queue.poll(100, TimeUnit.MILLISECONDS);
                if (item != null) {
                    item.setProcessingStartTime(Instant.now());

                    // Simulate variable processing time
                    simulateProcessing(item);

                    item.setProcessingEndTime(Instant.now());
                    processedCount.incrementAndGet();
                    lastItemCollector.execute(() -> lastItems.add(item));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void simulateProcessing(WorkItem item) {
         int processingTime = 50 + random.nextInt(100); // 50-150ms
        try {
            System.out.println(Arrays.toString(item.getPayload()) + " is processed");
            Thread.sleep(processingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }
}