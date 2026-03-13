package com.humanizar.acolhimento;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.jpa.properties.hibernate.hbm2ddl.auto=none",
		"spring.jpa.generate-ddl=false",
		"spring.rabbitmq.listener.simple.auto-startup=false",
		"spring.rabbitmq.listener.direct.auto-startup=false",
		"spring.task.scheduling.enabled=false"
})
class HumanizarAcolhimentoApplicationTests {

	@Test
	void contextLoads() {
	}

}
