package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Saving account — earns interest over time")
public class SavingAccountDTO extends AccountDTO {
}
