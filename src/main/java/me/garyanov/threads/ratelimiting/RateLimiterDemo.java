package me.garyanov.threads.ratelimiting;

import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.limiter.TokenBucketRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        ExecutorService producerExecutor = Executors.newFixedThreadPool(numProducers);
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(numConsumers);
        ScheduledExecutorService monitoringExecutor = Executors.newSingleThreadScheduledExecutor();

        List<AdaptiveProducer> producers = new ArrayList<>();
        for (int i = 0; i < numProducers; i++) {
            AdaptiveProducer producer = new AdaptiveProducer("producer-" + i, queue, rateLimiter);
            producers.add(producer);
            producerExecutor.execute(producer);
        }

        List<AdaptiveConsumer> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            AdaptiveConsumer consumer = new AdaptiveConsumer("consumer-" + i, queue, rateLimiter);
            consumers.add(consumer);
            consumerExecutor.execute(consumer);
        }

        var metricsCollector = new MetricsCollector(queue, producers, consumers, monitoringExecutor);
        metricsCollector.startMonitoring(rateLimiter);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Try to stop demo");
        shutdownExecutor(producerExecutor, "Producer Executor");
        shutdownExecutor(consumerExecutor, "Consumer Executor");
        shutdownExecutor(monitoringExecutor, "Monitoring Executor");
    }

    private static void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println(name + " did not terminate in time, forcing shutdown...");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println(name + " could not be terminated");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}