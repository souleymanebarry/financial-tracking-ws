package com.barry.bank.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Paginated operation history for a bank account")
public class AccountHistoryDTO {

    @Schema(description = "Unique identifier of the account", example = "7ba95f32-1234-4321-abcd-2c963f66afa6", accessMode = Schema.AccessMode.READ_ONLY)
    private UUID accountId;

    @Schema(description = "Last name of the account holder", example = "Dupont")
    private String accountHolderName;

    @Schema(description = "Current balance of the account", example = "2500.00")
    private BigDecimal balance;

    @Schema(description = "List of operations for the current page")
    private List<OperationDTO> operations;

    @Schema(description = "Current page index (0-based)", example = "0")
    private int currentPage;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Number of operations per page", example = "10")
    private int pageSize;

    @Schema(description = "Total number of operations across all pages", example = "47")
    private long totalElements;
}
