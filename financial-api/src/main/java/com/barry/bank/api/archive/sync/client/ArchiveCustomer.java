package com.barry.bank.api.archive.sync.client;

import com.barry.bank.api.archive.sync.dtos.CustomerArchiveDTO;
import com.barry.bank.api.exception.ArchiveServiceException;
import com.barry.bank.api.security.config.JwtService;
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

        restClient.post()
                .uri(archiveServiceUrl + "/api/v1/archives/customers")
                .headers(header -> header.setBearerAuth(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                    throw new ArchiveServiceException(
                            "Archive service returned " + response.getStatusCode()
                            + " for customerId=" + dto.getCustomerId()
                    );
                })
                .toBodilessEntity();
    }
}
