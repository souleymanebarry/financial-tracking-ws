package com.barry.bank.financial.tracking_ws.archive.sync.client;

import com.barry.bank.financial.tracking_ws.archive.sync.dtos.CustomerArchiveDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ArchiveCustomer {

    private final RestClient restClient;

    @Value("${archive.service.url}")
    private String archiveServiceUrl;

    public void archiveCustomerData(CustomerArchiveDTO customerArchiveDTO) {
        restClient.post()
                .uri(archiveServiceUrl + "/api/v1/archives/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(customerArchiveDTO)
                .retrieve()
                .toBodilessEntity();
    }
}
