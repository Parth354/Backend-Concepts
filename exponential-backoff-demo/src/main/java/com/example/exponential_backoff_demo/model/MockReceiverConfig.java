package com.example.exponential_backoff_demo.model;

import org.springframework.stereotype.Component;

@Component // This tells the spring to actually create a bean for this class
public class MockReceiverConfig {
    private int statusCode = 500; //HTTP Status to return
    private long delayMs = 0; //Forced delay in ms
    private int failuresBeforeSuccess = 2; //Fail N times, then auto-succeed
    private int currentFailures = 0;

    // The methods below are synchronized because a singleton instance of the config class will be created so to prevent the race conditions synchronized is necessary

    public synchronized void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public synchronized int getStatusCode() {
        return statusCode;
    }

    public synchronized long getDelayMs() {
        return delayMs;
    }

    public synchronized void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public synchronized int getFailuresBeforeSuccess() {
        return failuresBeforeSuccess;
    }

    public synchronized void setFailuresBeforeSuccess(int failuresBeforeSuccess) {
        this.failuresBeforeSuccess = failuresBeforeSuccess;
        this.currentFailures = 0;
    }

    public synchronized boolean shouldFail() {
        if(currentFailures < failuresBeforeSuccess ) {
            currentFailures++;
            return true;
        }
        return false;
    }

    public synchronized void reset() {
        this.currentFailures = 0;
    }
}
