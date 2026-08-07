package com.example.exponential_backoff_demo.model;


import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

// Used by React UI to actually see the Webhook Logs
public class WebhookLog {
    private final String timestamp;
    private final String webhookId;
    private final int attempt;
    private final String status;
    private final String message;
    private final long delayMs;

    public WebhookLog(String webhookId , int attempt , String status ,String message ,long delayMs) {
        timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
        this.webhookId =webhookId;
        this.attempt=attempt;
        this.delayMs=delayMs;
        this.message=message;
        this.status=status;
    }

    public int getAttempt() {
        return attempt;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getWebhookId() {
        return webhookId;
    }
}
