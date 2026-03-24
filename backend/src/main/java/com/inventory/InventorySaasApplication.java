package com.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.inventory.config.JwtConfig;
import com.inventory.config.CorsProperties;

@SpringBootApplication
@EnableConfigurationProperties({ JwtConfig.class, CorsProperties.class })
public class InventorySaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventorySaasApplication.class, args);
	}

}
