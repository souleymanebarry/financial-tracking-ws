package com.barry.bank.financial.tracking_ws.archive.sync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Log4j2
public class RestClientConfig {

    @Bean
    public RestClient restClient(ObjectMapper objectMapper) {
        return RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    try {

                        byte[] jsonBytes = objectMapper.writeValueAsBytes(body);

                        double sizeKO = jsonBytes.length / 1024.0;
                        double sizeMO = jsonBytes.length / (1024.0 * 1024.0);
                        log.info("📦 Payload size = {} octets | {} Ko | {} Mo",
                                jsonBytes.length,
                                String.format("%.2f", sizeKO),
                                String.format("%.4f", sizeMO));
                    } catch (Exception e) {
                        log.warn("❗ Unable to calculate payload size : {}", e.getMessage());
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
