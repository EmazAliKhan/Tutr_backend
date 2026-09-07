package com.tutr.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TutrBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TutrBackendApplication.class, args);
	}

}
