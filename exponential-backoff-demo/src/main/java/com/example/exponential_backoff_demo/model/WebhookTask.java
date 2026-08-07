package com.example.exponential_backoff_demo.model;

import java.util.UUID;

// This object represents a single webhook attempt in our queue
// Comparable is used here so that the PriorityQueue storing Tasks has method to call to decide which task object should come first
public class WebhookTask implements Comparable<WebhookTask> {
    private final String webhookId; // Unique ID for idempotency to achieve receive once
    private final String targetUrl;
    private final String payload;
    private final int attempt;
    private final long executeTimestamp;

    public WebhookTask(String targetUrl , String payload , int attempt , long delayMs ) {
        this.targetUrl = targetUrl;
        this.webhookId = "evt_" + UUID.randomUUID().toString().substring(0, 8);
        this.attempt = attempt;
        this.payload = payload;

        //Current time + delay = future execution time
        this.executeTimestamp = System.currentTimeMillis() + delayMs;
    }

    // Secondary Constructor for retries reusing the same webhookID
    public WebhookTask(String targetUrl , String payload , int attempt , long delayMs, String webhookId ) {
        this.targetUrl = targetUrl;
        this.webhookId = webhookId;
        this.attempt = attempt;
        this.payload = payload;

        //Current time + delay = future execution time
        this.executeTimestamp = System.currentTimeMillis() + delayMs;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public int getAttempt() {
        return attempt;
    }

    public long getExecuteTimestamp() {
        return executeTimestamp;
    }

    public String getPayload() {
        return payload;
    }

    public String getWebhookId() {
        return webhookId;
    }

    //Compares two tasks so the queue always puts the task with the smallest timestamp at the top
    @Override
    public int compareTo (WebhookTask other ) {
        return Long.compare(this.executeTimestamp , other.executeTimestamp);
    }
}
