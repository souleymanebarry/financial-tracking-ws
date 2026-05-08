package com.barry.bank.financial.tracking_ws.archive.sync.client;

import com.barry.bank.financial.tracking_ws.archive.sync.dtos.CustomerArchiveDTO;
import com.barry.bank.financial.tracking_ws.config.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Log4j2
public class ArchiveCustomer {

    private final JwtService jwtService;
    private final RestClient restClient;

    @Value("${archive.service.url}")
    private String archiveServiceUrl;

    public void archiveCustomerData(CustomerArchiveDTO dto) {
        String accessToken = jwtService.generateServiceToken(dto.getCustomerId().toString());
        log.info("JWT generated for customerId= {}", dto.getCustomerId());
        log.info("Calling archive-service with token: {}", accessToken);

        restClient.post()
                .uri(archiveServiceUrl + "/api/v1/archives/customers")
                .headers(header -> header.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }
}
