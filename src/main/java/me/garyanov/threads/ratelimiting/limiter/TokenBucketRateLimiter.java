package me.garyanov.threads.ratelimiting.limiter;

import me.garyanov.threads.ratelimiting.model.SystemMetrics;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements DynamicRateLimiter {
    private final double maxRate; // Maximum allowed rate
    private final int maxBurst; // Maximum burst capacity
    private volatile double currentRate;
    private double tokens;
    private volatile long lastRefillTime;
    private final ReentrantLock lock = new ReentrantLock();
    Random random = new Random();


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

    @Override
    public void updateRateLimit(SystemMetrics metrics) {
        double newRate = calculateOptimalRate(metrics);
        if (newRate == currentRate) {
            return;
        }
        lock.lock();
        try {
            System.out.printf("Rate adjusted: %.2f -> %.2f items/sec (queue: %d)%n",
                    currentRate, newRate, metrics.queueSize());
            this.currentRate = Math.min(maxRate, Math.max(1, newRate));
            // Log rate adjustment for observability

        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getCurrentRate() {
        return currentRate;
    }

    @Override
    public int getCurrentBurst() {
        return 0;
    }

    @Override
    public long calculateNextInterval() {
        double rate = getCurrentRate();
        double intervalMs = 1000.0 / rate;
        // Add ±20% jitter to simulate real-world variability
        double jitter = 0.2 * intervalMs * (random.nextDouble() * 2 - 1);
        return Math.max(1, (long) (intervalMs + jitter));
    }

    private void refillTokens() {
        long now = System.nanoTime();
        long timeElapsed = now - lastRefillTime;
        //System.out.println("time elapsed = " + timeElapsed);
        double tokensToAdd = (timeElapsed / 1_000_000_000.0) * currentRate;
       // System.out.println(" tokens to add = " + tokensToAdd);

        lock.lock();
        try {
            //System.out.println("tokens = " + tokens);
            tokens = Math.min(maxBurst, tokens + tokensToAdd);
            //System.out.println("tokens after refill = " + tokens);
            lastRefillTime = now;
        } finally {
            lock.unlock();
        }
    }


    private double calculateOptimalRate(SystemMetrics metrics) {
        double queueUtilization = (double) metrics.queueSize() / metrics.queueCapacity();
        int currentThroughput = metrics.systemThroughput();

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
        if (metrics.avgProcessingLatency() > 1000) { // 1 second threshold
            return currentRate * 0.9; // Reduce rate if latency is high
        }

        return currentRate; // No change
    }
}
