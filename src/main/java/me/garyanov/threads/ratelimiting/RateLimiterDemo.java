package me.garyanov.threads.ratelimiting;

import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.limiter.TokenBucketRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RateLimiterDemo {
    public static void main(String[] args) {
        int queueCapacity = 1000;
        int numProducers = 3;
        int numConsumers = 2;
        double initialRate = 50.0; // items/sec
        double maxRate = 200.0;
        int maxBurst = 100;

        BlockingQueue<WorkItem> queue = new LinkedBlockingQueue<>(queueCapacity);
        DynamicRateLimiter rateLimiter = new TokenBucketRateLimiter(initialRate, maxRate, maxBurst);

        List<AdaptiveProducer> producers = new ArrayList<>();
        for (int i = 0; i < numProducers; i++) {
            AdaptiveProducer producer = new AdaptiveProducer("producer-" + i, queue, rateLimiter);
            producers.add(producer);
            new Thread(producer).start();
        }

        List<AdaptiveConsumer> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            AdaptiveConsumer consumer = new AdaptiveConsumer("consumer-" + i, queue, rateLimiter);
            consumers.add(consumer);
            new Thread(consumer).start();
        }

        var metricsCollector = new MetricsCollector(queue, producers, consumers, );
        metricsCollector.startMonitoring(rateLimiter);

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        producers.forEach(AdaptiveProducer::stop);
        consumers.forEach(AdaptiveConsumer::stop);
    }
}