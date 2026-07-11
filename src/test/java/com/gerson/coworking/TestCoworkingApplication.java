package com.gerson.coworking;

import org.springframework.boot.SpringApplication;

public class TestCoworkingApplication {

	public static void main(String[] args) {
		SpringApplication.from(CoworkingApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
