package com.barry.bank.financial.tracking_ws.archive.sync.dtos;

import com.barry.bank.financial.tracking_ws.enums.Gender;
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

    private Gender gender;

    private List<AccountArchiveDTO> accounts;

}
