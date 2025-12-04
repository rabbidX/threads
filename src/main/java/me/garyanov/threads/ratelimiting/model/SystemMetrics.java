package main.java.me.garyanov.threads.ratelimiting.model;

import lombok.Data;

import java.time.Instant;

@Data
public class SystemMetrics {
    private final int queueSize;
    private final double producerRate; // items/sec
    private final double consumerRate; // items/sec
    private final double avgProcessingLatency;
    private final double systemThroughput;
    private final Instant timestamp;
}
