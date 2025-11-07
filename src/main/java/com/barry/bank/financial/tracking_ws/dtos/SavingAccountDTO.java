package com.barry.bank.financial.tracking_ws.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
public class SavingAccountDTO extends AccountDTO {

    private BigDecimal interestRate;

}
