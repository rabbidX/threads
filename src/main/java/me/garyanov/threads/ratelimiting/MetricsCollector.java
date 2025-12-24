package me.garyanov.threads.ratelimiting;

import lombok.Data;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.SystemMetrics;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
public class MetricsCollector {
    private final BlockingQueue<WorkItem> queue;
    private final List<AdaptiveProducer> producers;
    private final List<AdaptiveConsumer> consumers;
    private final ScheduledExecutorService scheduler;

    public void startMonitoring(DynamicRateLimiter rateLimiter) {
        scheduler.scheduleAtFixedRate(() -> {
            SystemMetrics metrics = collectMetrics();
            rateLimiter.updateRateLimit(metrics);
            logMetrics(metrics);
        }, 1, 1, TimeUnit.SECONDS); // Adjust every second
    }

    public void stopMonitoring() {
        scheduler.shutdown();
    }

    private void logMetrics(SystemMetrics metrics) {
    }

    private SystemMetrics collectMetrics() {
        int queueSize = queue.size();
        double producerRate = calculateProducerRate();
        double consumerRate = calculateConsumerRate();
        double avgLatency = calculateAverageLatency();
        double systemThroughput = calculateSystemThroughput();

        return new SystemMetrics(queueSize, producerRate, consumerRate, avgLatency, systemThroughput, Instant.now());
    }

    private double calculateAverageLatency() {
        return 0;
    }

    private double calculateConsumerRate() {
        return 0;
    }

    private double calculateProducerRate() {
        return 0;
    }

    private double calculateSystemThroughput() {
        return 0;
    }
}
