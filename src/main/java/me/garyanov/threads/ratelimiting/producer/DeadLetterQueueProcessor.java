package me.garyanov.threads.ratelimiting.producer;

import lombok.AllArgsConstructor;
import me.garyanov.threads.ratelimiting.limiter.DynamicRateLimiter;
import me.garyanov.threads.ratelimiting.model.WorkItem;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

@AllArgsConstructor
public class DeadLetterQueueProcessor implements Producer{
    private final BlockingQueue<WorkItem> queue;
    private final BlockingQueue<WorkItem> deadLetterQueue;
    private final DynamicRateLimiter rateLimiter;

    @Override
    public void run() {
        while (running.get()) {
            var item = deadLetterQueue.peek();
            if (item != null && rateLimiter.tryAcquire()) {
                boolean offered = queue.offer(item);
                if (offered) {
                    deadLetterQueue.remove();
                    System.out.println(Arrays.toString(item.getPayload()) + " is offered from Dead Letter Queue");
                }
            }
            try {
                Thread.sleep(rateLimiter.calculateNextInterval());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
