package com.gerson.coworking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CoworkingApplication {

	public static void main(String[] args) {
		SpringApplication.run(CoworkingApplication.class, args);
	}

}
