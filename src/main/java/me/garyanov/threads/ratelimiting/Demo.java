package me.garyanov.threads.ratelimiting;

import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.limiter.TokenBucketRateLimiter;
import me.garyanov.threads.ratelimiting.model.LastProcessedItemsCollection;
import me.garyanov.threads.ratelimiting.model.WorkItem;
import me.garyanov.threads.ratelimiting.producer.AdaptiveProducer;
import me.garyanov.threads.ratelimiting.producer.DeadLetterQueueProcessor;
import me.garyanov.threads.ratelimiting.producer.Producer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class Demo {

    protected static void run(int queueCapacity, int numProducers, int numConsumers, double initialRate, double maxRate, int maxBurst) {
        BlockingQueue<WorkItem> queue = new ArrayBlockingQueue<>(queueCapacity);
        BlockingQueue<WorkItem> deadLetterQueue = new ArrayBlockingQueue<>(queueCapacity * 1000);
        DynamicRateLimiter rateLimiter = new TokenBucketRateLimiter(initialRate, maxRate, maxBurst);
        ExecutorService producerExecutor = Executors.newFixedThreadPool(numProducers + 1);
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(numConsumers);
        ExecutorService lastItemsCollector = Executors.newFixedThreadPool(numConsumers);
        ScheduledExecutorService monitoringExecutor = Executors.newSingleThreadScheduledExecutor();
        LastProcessedItemsCollection lastItems = new LastProcessedItemsCollection();

        List<Producer> producers = new ArrayList<>();
        for (int i = 0; i < numProducers; i++) {
            AdaptiveProducer producer = new AdaptiveProducer("producer-" + i, queue, deadLetterQueue, rateLimiter);
            producers.add(producer);
            producerExecutor.execute(producer);
        }
        DeadLetterQueueProcessor deadLetterQueueProducer = new DeadLetterQueueProcessor(queue, deadLetterQueue, rateLimiter);
        producers.add(deadLetterQueueProducer);
        producerExecutor.execute(deadLetterQueueProducer);

        List<AdaptiveConsumer> consumers = new ArrayList<>();
        for (int i = 0; i < numConsumers; i++) {
            AdaptiveConsumer consumer = new AdaptiveConsumer("consumer-" + i, queue, lastItemsCollector, lastItems);
            consumers.add(consumer);
            consumerExecutor.execute(consumer);
        }

        var metricsCollector = new MetricsCollector(queue, producers, consumers, monitoringExecutor, lastItems);
        metricsCollector.startMonitoring(rateLimiter);

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Try to stop demo");
        producers.forEach(Producer::stop);
        consumers.forEach(AdaptiveConsumer::stop);
        shutdownExecutor(producerExecutor, "Producer Executor");
        shutdownExecutor(consumerExecutor, "Consumer Executor");
        shutdownExecutor(lastItemsCollector, "Last items collector");
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
