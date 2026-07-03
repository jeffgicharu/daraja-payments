package com.jeffgicharu.daraja;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DarajaPaymentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DarajaPaymentsApplication.class, args);
	}

}
