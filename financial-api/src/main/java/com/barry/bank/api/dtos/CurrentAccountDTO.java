package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Current account — includes overdraft facility")
public class CurrentAccountDTO extends AccountDTO {
}
