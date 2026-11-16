package br.com.newelog.motoristas;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MotoristasServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MotoristasServiceApplication.class, args);
	}

}
