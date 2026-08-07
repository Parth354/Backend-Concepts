package com.example.exponential_backoff_demo;

import com.example.exponential_backoff_demo.service.WebhookService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableScheduling // Enables the background scheduling engine for @Scheduled
public class ExponentialBackoffDemoApplication {


	//Counter to simulate a dummy endpoint that fails twice
	public static void main(String[] args) {
		SpringApplication.run(ExponentialBackoffDemoApplication.class, args);
	}

}
