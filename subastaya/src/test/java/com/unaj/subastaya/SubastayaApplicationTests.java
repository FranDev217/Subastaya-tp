package com.unaj.subastaya;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "subastaya.worker.initial-delay-ms=600000")
class SubastayaApplicationTests {

	@Test
	void contextLoads() {
	}

}
