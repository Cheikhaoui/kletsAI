package com.kletsai;

import org.springframework.boot.SpringApplication;

public class TestKletsAiApplication {

	public static void main(String[] args) {
		SpringApplication.from(KletsAiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
