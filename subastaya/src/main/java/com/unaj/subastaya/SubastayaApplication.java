package com.unaj.subastaya;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class SubastayaApplication {

	private static final String ZONA_HORARIA = "America/Argentina/Buenos_Aires";

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
		SpringApplication.run(SubastayaApplication.class, args);
	}

}
