package com.barry.bank.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.barry.bank")
@EntityScan(basePackages = "com.barry.bank.domain.model")
@EnableJpaRepositories(basePackages = "com.barry.bank.persistence.repositories")
@EnableScheduling
public class FinancialBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancialBatchApplication.class, args);
    }
}
