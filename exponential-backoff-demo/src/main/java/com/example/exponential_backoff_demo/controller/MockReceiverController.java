package com.example.exponential_backoff_demo.controller;

import com.example.exponential_backoff_demo.model.MockReceiverConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/mock-receiver")
@CrossOrigin(origins = "*") // Allows React UI to access the endpoint
public class MockReceiverController {

    private final MockReceiverConfig config;

    //Set to store processed Idempotency keys (Simulating receiver DB)
    private final Set<String> processedWebhookIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

     public MockReceiverController(MockReceiverConfig config) {
         this.config = config;
     }

     @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestHeader (value = "X-Webhook-ID", required = false) String webhookId ,
                                                 @RequestHeader (value = "X-Attempt-Number", required = false) String attempt )
         throws InterruptedException {
         System.out.printf("MOCK RECEIVER : Request Recieved. ID: %s , Attempt %s%n" ,webhookId,attempt);

         //CASE 1: Check Idempotency (the receiver already processed this ?)
         if(webhookId != null && processedWebhookIds.contains(webhookId)) {
             return ResponseEntity.ok("Duplicate request ignored safely.");
         }

         // CASE 2: Simulate User-Controlled Delay
         if (config.getDelayMs() > 0 ) {
             Thread.sleep(config.getDelayMs());
         }

         //CASE 3: Simulate Fail N times and then succeed
         if(config.shouldFail()) {
             int code = config.getStatusCode();

             if(code == 429) {
                 HttpHeaders headers = new HttpHeaders();
                 headers.add("Retry-After" , "3");
                 return ResponseEntity.status(429).headers(headers).body("Too Many Requests");
             }
            return ResponseEntity.status(code).body("Simulated Error");
         }

         // Mark as processed in our Idempotency
         if(webhookId != null ) {
             processedWebhookIds.add(webhookId);
         }
         System.out.println("Processing Success!");
         return ResponseEntity.ok("Success");
     }

     // Endpoint for React UI to adjust receiver behavior live
    @PostMapping("/configure")
    public String configure(@RequestBody MockReceiverConfig newConfig) {
         config.setStatusCode(newConfig.getStatusCode());
         config.setDelayMs(newConfig.getDelayMs());
         config.setFailuresBeforeSuccess(newConfig.getFailuresBeforeSuccess());
         processedWebhookIds.clear();
         return "Mock receiver updated successfully!";
    }

}
