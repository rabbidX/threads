package me.garyanov.threads.racecondition;

import me.garyanov.threads.racecondition.queue.ConcurrentNaturalNumbersQueue;
import me.garyanov.threads.racecondition.queue.NaturalNumbersQueue;

public class RaceConditionDemo {
    public static void main(String[] args) {
       var queue = new NaturalNumbersQueue();
       var concurrentQueue = new ConcurrentNaturalNumbersQueue();

       var adder = new MultithreadedQueueAdder(queue);
       var concurrentAdder = new MultithreadedQueueAdder(concurrentQueue);

       System.out.println("Naive adder sum = " + adder.getSum());
       System.out.println("Concurrent adder sum = " + concurrentAdder.getSum());
       System.out.println("Accurate sum = "+ queue.getSum());
    }
}
