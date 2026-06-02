package com.barry.bank.api.dtos;

import com.barry.bank.api.validation.OnCreate;
import com.barry.bank.domain.entities.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
@Schema(description = "Customer information")
public class CustomerDTO {

    @Schema(description = "Unique identifier of the customer", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID customerId;

    @NotBlank(groups = OnCreate.class)
    @Schema(description = "First name of the customer", example = "Jean", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotBlank(groups = OnCreate.class)
    @Schema(description = "Last name of the customer", example = "Dupont", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Email
    @NotBlank
    @Schema(description = "Email address of the customer", example = "jean.dupont@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Gender of the customer", example = "MALE", allowableValues = {"MALE", "FEMALE"})
    private Gender gender;
}
