package com.ambulance.ambulance_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class AmbulanceServiceApplication {

	public static void main(String[] args) {
		// Load .env file before Spring starts
		Dotenv dotenv = Dotenv.configure().load();
		dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
		SpringApplication.run(AmbulanceServiceApplication.class, args);
	}

}
