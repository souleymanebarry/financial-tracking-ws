package com.barry.bank.financial.tracking_ws.dtos;

import com.barry.bank.financial.tracking_ws.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class CustomerDTO {

    private UUID customerId;

    private String firstName;

    private String lastName;

    @Email
    @NotBlank
    private String email;

    private Gender gender;
}
