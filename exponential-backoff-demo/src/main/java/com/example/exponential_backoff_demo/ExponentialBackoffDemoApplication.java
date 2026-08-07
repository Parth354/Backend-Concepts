package com.example.exponential_backoff_demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling // Enables the background scheduling engine for @Scheduled
public class ExponentialBackoffDemoApplication {


	//Counter to simulate a dummy endpoint that fails twice
	public static void main(String[] args) {
		SpringApplication.run(ExponentialBackoffDemoApplication.class, args);
	}

}
