package com.barry.bank.api;


import com.barry.bank.api.security.config.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableConfigurationProperties(RsaKeyProperties.class)
@SpringBootApplication(scanBasePackages = "com.barry.bank")
@EntityScan(basePackages = "com.barry.bank.domain.model")
@EnableJpaRepositories(basePackages = "com.barry.bank.persistence.repositories")
public class FinancialTrackingWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTrackingWsApplication.class, args);
	}
}
