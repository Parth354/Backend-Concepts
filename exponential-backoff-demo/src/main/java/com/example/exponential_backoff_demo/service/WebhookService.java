package com.example.exponential_backoff_demo.service;

import com.example.exponential_backoff_demo.model.WebhookLog;
import com.example.exponential_backoff_demo.model.WebhookTask;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Service // Tells Spring to instantiate and manage this class as a singleton Bean
public class WebhookService {

    // Helper Object for sending HTTP POST requests
    private final RestTemplate restTemplate;

    // Thread - safe Priority Queue sorted automatically using WebhookTask.compareTo()
    private final PriorityBlockingQueue<WebhookTask> taskQueue = new PriorityBlockingQueue<>();

    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_DELAY_MS = 1000; // Starts at 1 second
    private static final double MULTIPLIER = 2.0;  // Doubles each attempt
    private static final int HTTP_TIMEOUT_MS = 2000; //Strict 2-second timeout

    // Thread-safe Log memory for the React UI to read from
    private final ConcurrentLinkedQueue<WebhookLog> logHistory = new ConcurrentLinkedQueue<>();


    public WebhookService() {
        // Configures HTTP client with explicit Connect and Read timeouts!
        //Java throws a Resource Access Exception (Read Timeout ) allowing our catch block to handle it safely
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(HTTP_TIMEOUT_MS);
        factory.setReadTimeout(HTTP_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    //Public method triggered when a webhook request is initiated
    public void dispatchWebhook (String targetUrl , String payload ) {
        WebhookTask task = new WebhookTask(targetUrl , payload ,0, 0);
        addLog(task.getWebhookId() , 1 ,"QUEUED" , "Initial Webhook Dispatch Queued" ,0);
        executeOrQueue(task);
    }

    private void executeOrQueue(WebhookTask task) {
        int attemptNumber = task.getAttempt()+1;
        try {
            addLog(task.getWebhookId() , attemptNumber ,"ATTEMPTING" , "Sending POST Request to " + task.getTargetUrl() ,0);
            System.out.printf("[Attempt %d] Sending POST request to %s%n" , task.getAttempt()+1 , task.getTargetUrl());

            // Add Idempotency & Attempt Headers
            HttpHeaders headers =  new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-ID" ,task.getWebhookId());
            headers.set("X-Attempt-Number" , String.valueOf(attemptNumber));

            HttpEntity<String> entity = new HttpEntity<>(task.getPayload(),headers);

            //Sending the actual HTTP POST request
            ResponseEntity<String> response = restTemplate.postForEntity(task.getTargetUrl() ,entity , String.class);

            if(response.getStatusCode().is2xxSuccessful()) {
                addLog(task.getWebhookId(), attemptNumber,"SUCCESS" , "Delivered! Receiver returned HTTP" + response.getStatusCode().value(),0);
            }

        } catch (HttpClientErrorException e) {
            // Edge Case : 4xx Client Errors
            if(e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS ) {
                // Respect "Retry -After" Header if server gave 429
                long customDelay = parseRetryAfterHeader(e.getResponseHeaders());
                addLog(task.getWebhookId() , attemptNumber , "FAILED_429" , "Rate Limited(429). Retrying after custom delay" , customDelay) ;
                scheduleRetry(task,customDelay);
            }else {
                //Non-retryable error! (400 ,401 ,404) -> Abort
                addLog(task.getWebhookId() , attemptNumber , "ABORTED_NON_RETRYABLE" , "Fatal Client error (HTTP " + e.getStatusCode().value()+ ").Retry canceled",0);
            }
        } catch (HttpServerErrorException e ) {
            // 5xx Server Errors -> Retry
            addLog(task.getWebhookId(), attemptNumber, "FAILED_5XX" , "Server Error" ,0);
            handleExponentialRetry(task);
        }
        catch (ResourceAccessException e) {
            // Network Failure . Read Timeout ( Server took > 2000 ms)
            addLog(task.getWebhookId() , attemptNumber , "FAILED_TIMEOUT" , "Network Timeout!" , 0);
            handleExponentialRetry(task);
        } catch (Exception e) {
            addLog(task.getWebhookId() , attemptNumber , "FAILED_UNKNOWN" , e.getMessage() , 0);
            handleExponentialRetry(task);
        }
    }

    private void handleExponentialRetry(WebhookTask failedTask) {

        int nextAttempt = failedTask.getAttempt() +1;

        if(nextAttempt<= MAX_RETRIES ) {
            //Formula : initialDelay * (multiplier ^ currentAttempt)
            long backOffDelay = (long) (INITIAL_DELAY_MS * Math.pow(MULTIPLIER , failedTask.getAttempt()));

            scheduleRetry(failedTask,backOffDelay);
        }
        else {
            addLog(failedTask.getWebhookId(),nextAttempt ,"DLQ","Max retries reached. Moved to Dead Queue" ,0);
        }
    }

    private void scheduleRetry(WebhookTask failedTask,long delayMs ) {
        int nextAttempt = failedTask.getAttempt() +1;
        WebhookTask retryTask = new WebhookTask(
                failedTask.getTargetUrl(),
                failedTask.getPayload(),
                nextAttempt,
                delayMs,
                failedTask.getWebhookId()
        );

        addLog(failedTask.getWebhookId() , nextAttempt+1 ,"SCHEDULED" ,"Retry in " +delayMs +"ms",delayMs);
        taskQueue.add(retryTask);
    }

    private long parseRetryAfterHeader(HttpHeaders headers) {
        if(headers != null && headers.getFirst("Retry-After") != null ) {
            try {
                return Long.parseLong(Objects.requireNonNull(headers.getFirst("Retry-After"))) * 1000;
            } catch(NumberFormatException ignored){}
        }
        return 3000; // Default fallback delay;
    }

    // Runs automatically every 500ms in a background thread
    @Scheduled(fixedDelay =200)
    public void processQueue() {
        long now = System.currentTimeMillis();

        // Check if the top task in the queue is ready to run
        while(!taskQueue.isEmpty() && taskQueue.peek().getExecuteTimestamp() <= now ) {
            WebhookTask task = taskQueue.poll(); // Remove the task from queue
            if(task != null ) {
                executeOrQueue(task);
            }
        }
    }

    public List<WebhookLog> getLogs() {
        return new ArrayList<>(logHistory);
    }

    public void clearLogs() {
        logHistory.clear();
    }
    private void addLog(String webhookId , int attempt , String status, String message , long delayMs) {
        logHistory.add(new WebhookLog(webhookId,attempt,status,message,delayMs));
    }
}
