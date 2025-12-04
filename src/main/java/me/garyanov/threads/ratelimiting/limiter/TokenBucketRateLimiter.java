package me.garyanov.threads.ratelimiting.limiter;

import me.garyanov.threads.ratelimiting.model.SystemMetrics;

import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements DynamicRateLimiter {
    private final double maxRate; // Maximum allowed rate
    private final int maxBurst; // Maximum burst capacity
    private volatile double currentRate;
    private volatile double tokens;
    private volatile long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();

    public TokenBucketRateLimiter(double initialRate, double maxRate, int maxBurst) {
        this.currentRate = initialRate;
        this.maxRate = maxRate;
        this.maxBurst = maxBurst;
        this.tokens = maxBurst;
        this.lastRefillTime = System.nanoTime();
    }

    @Override
    public boolean tryAcquire() {
        refillTokens();

        lock.lock();
        try {
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    private void refillTokens() {
        long now = System.nanoTime();
        long timeElapsed = now - lastRefillTime;
        double tokensToAdd = (timeElapsed / 1_000_000_000.0) * currentRate;

        lock.lock();
        try {
            tokens = Math.min(maxBurst, tokens + tokensToAdd);
            lastRefillTime = now;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateRateLimit(SystemMetrics metrics) {
        double newRate = calculateOptimalRate(metrics);

        lock.lock();
        try {
            this.currentRate = Math.min(maxRate, Math.max(1, newRate));
            // Log rate adjustment for observability
            System.out.printf("Rate adjusted: %.2f -> %.2f items/sec (queue: %d)%n",
                    currentRate, newRate, metrics.getQueueSize());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getCurrentRate() {
        return 0;
    }

    @Override
    public int getCurrentBurst() {
        return 0;
    }

    private double calculateOptimalRate(SystemMetrics metrics) {
        double queueUtilization = (double) metrics.getQueueSize() / metrics.getQueueSize();
        double currentThroughput = metrics.getSystemThroughput();

        // Control algorithm - you can implement different strategies:

        // Strategy 1: Queue-based control
        if (queueUtilization > 0.8) {
            // Queue is filling up - reduce rate
            return currentRate * 0.8;
        } else if (queueUtilization < 0.2 && currentThroughput < currentRate) {
            // Queue is emptying and we're not hitting limits - increase rate
            return currentRate * 1.1;
        }

        // Strategy 2: Latency-based control
        if (metrics.getAvgProcessingLatency() > 1000) { // 1 second threshold
            return currentRate * 0.9; // Reduce rate if latency is high
        }

        return currentRate; // No change
    }
}
