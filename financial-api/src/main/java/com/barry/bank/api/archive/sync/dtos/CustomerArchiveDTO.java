package com.barry.bank.api.archive.sync.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CustomerArchiveDTO {

    private UUID customerId;

    private String firstName;

    private String lastName;

    private String email;

    private String gender;

    private List<AccountArchiveDTO> accounts;

}
