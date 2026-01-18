package me.garyanov.threads.ratelimiting.model;

import java.time.Instant;

public record SystemMetrics(
        int queueSize,
        int queueCapacity,
        double avgProcessingLatency,
        int systemThroughput,
        Instant timestamp) {
}
