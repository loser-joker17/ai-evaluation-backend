package com.aievaluation.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AiEvaluationBackendApplication {

	public static void main(String[] args) {
		// Store and process all times in UTC; the frontend shows them in the user's timezone.
		// Also avoids Windows' legacy "Asia/Calcutta" id, which PostgreSQL rejects.
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		SpringApplication.run(AiEvaluationBackendApplication.class, args);
	}
}