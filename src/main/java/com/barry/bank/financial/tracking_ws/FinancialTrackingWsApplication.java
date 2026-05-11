package com.barry.bank.financial.tracking_ws;

import com.barry.bank.financial.tracking_ws.config.RsaKeyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(RsaKeyProperties.class)
@SpringBootApplication
public class FinancialTrackingWsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinancialTrackingWsApplication.class, args);
	}
}
