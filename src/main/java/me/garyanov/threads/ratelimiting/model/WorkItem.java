package main.java.me.garyanov.threads.ratelimiting.model;

import lombok.Data;

import java.time.Instant;

@Data
public class WorkItem {
    private final String id;
    private final Instant createdTime;
    private final String producerId;
    private final byte[] payload;
    private volatile Instant processingStartTime;
    private volatile Instant processingEndTime;
}
