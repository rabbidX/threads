package me.garyanov.threads.ratelimiting;

public class BackpressureDemo extends Demo{
    public static void main(String[] args) {
        int queueCapacity = 10;
        int numProducers = 3;
        int numConsumers = 2;
        double initialRate = 500.0; // items/sec
        double maxRate = 2000.0;
        int maxBurst = 1000;
        run(queueCapacity, numProducers, numConsumers, initialRate, maxRate, maxBurst);
    }
}
