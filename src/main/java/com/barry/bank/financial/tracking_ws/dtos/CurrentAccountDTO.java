package com.barry.bank.financial.tracking_ws.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
public class CurrentAccountDTO extends AccountDTO {

    private BigDecimal overDraft;

}
