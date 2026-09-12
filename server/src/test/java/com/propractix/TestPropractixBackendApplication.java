package com.propractix;

import org.springframework.boot.SpringApplication;

public class TestPropractixBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(PropractixBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
