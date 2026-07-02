package com.accel.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AccelApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccelApplication.class, args);
	}

}
