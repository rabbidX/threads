package me.garyanov.threads.ratelimiting;

import lombok.Data;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.LastProcessedItemsCollection;
import me.garyanov.threads.ratelimiting.model.SystemMetrics;
import me.garyanov.threads.ratelimiting.model.WorkItem;
import me.garyanov.threads.ratelimiting.producer.Producer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
public class MetricsCollector {
    private final BlockingQueue<WorkItem> queue;
    private final List<Producer> producers;
    private final List<AdaptiveConsumer> consumers;
    private final ScheduledExecutorService scheduler;
    private final LastProcessedItemsCollection lastItems;

    public void startMonitoring(DynamicRateLimiter rateLimiter) {
        scheduler.scheduleAtFixedRate(() -> {
            SystemMetrics metrics = collectMetrics();
            rateLimiter.updateRateLimit(metrics);
        }, 1, 1, TimeUnit.SECONDS); // Adjust every second
    }

    public void stopMonitoring() {
        scheduler.shutdown();
    }

    private SystemMetrics collectMetrics() {
        int queueSize = queue.size();
        int capacity = queue.remainingCapacity() + queueSize;
        double avgLatency = calculateAverageLatency();
        int systemThroughput = calculateSystemThroughput();

        return new SystemMetrics(queueSize, capacity, avgLatency, systemThroughput, Instant.now());
    }

    private double calculateAverageLatency() {
        return lastItems.getAverageProcessingTimeMillis();
    }

    private int calculateSystemThroughput() {
        return lastItems.getCountLastSecond();
    }
}
