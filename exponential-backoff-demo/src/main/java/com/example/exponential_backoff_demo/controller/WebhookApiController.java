package com.example.exponential_backoff_demo.controller;

import com.example.exponential_backoff_demo.model.WebhookLog;
import com.example.exponential_backoff_demo.service.WebhookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class WebhookApiController {

    private final WebhookService webhookService;

    public WebhookApiController(WebhookService webhookService){
        this.webhookService=webhookService;
    }

    @PostMapping("/trigger")
    public String triggerWebhook(@RequestParam(defaultValue = "http://localhost:8080/api/mock-receiver") String url ) {
        webhookService.dispatchWebhook(url , "{\"event\":\"order.created\",\"amount\":250}");
        return "Webhook Triggered";
    }

    @GetMapping("/logs")
    public List<WebhookLog> getLogs(){
        return webhookService.getLogs();
    }

    @DeleteMapping("/logs")
    public String clearLogs() {
        webhookService.clearLogs();
        return "Logs cleared";
    }

}
