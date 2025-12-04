package me.garyanov.threads.ratelimiting.limiter;

import me.garyanov.threads.ratelimiting.model.SystemMetrics;

public interface DynamicRateLimiter {
    /**
     * Try to acquire permission for one work item
     * @return true if allowed, false if rate limited
     */
    boolean tryAcquire();

    /**
     * Update rate limit based on current system metrics
     */
    void updateRateLimit(SystemMetrics metrics);

    /**
     * Get current permitted rate (items per second)
     */
    double getCurrentRate();

    /**
     * Get current burst capacity
     */
    int getCurrentBurst();
}
