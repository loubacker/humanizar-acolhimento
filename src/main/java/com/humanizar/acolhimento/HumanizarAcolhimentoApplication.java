package com.humanizar.acolhimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableResilientMethods
public class HumanizarAcolhimentoApplication {

	static void main(String[] args) {
		SpringApplication.run(HumanizarAcolhimentoApplication.class, args);
	}

}
