package me.garyanov.threads.ratelimiting.model;

import java.time.Instant;

/**
 * @param producerRate items/sec
 * @param consumerRate items/sec
 */
public record SystemMetrics(
        int queueSize,
        int queueCapacity,
        double producerRate,
        double consumerRate,
        double avgProcessingLatency,
        double systemThroughput,
        Instant timestamp) {
}
