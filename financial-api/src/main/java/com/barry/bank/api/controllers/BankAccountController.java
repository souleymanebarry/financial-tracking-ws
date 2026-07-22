package com.barry.bank.api.controllers;

import com.barry.bank.api.dtos.AccountDTO;
import com.barry.bank.api.dtos.AccountHistoryDTO;
import com.barry.bank.api.dtos.CreditRequestDTO;
import com.barry.bank.api.dtos.DebitRequestDTO;
import com.barry.bank.api.dtos.OperationDTO;
import com.barry.bank.api.dtos.TransferDTO;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Accounts",
        description = """
        Bank account management APIs.

        Includes:
        - Accounts listing (all / paginated)
        - Account details & history
        - Financial operations (debit, credit, transfer)
        - Account creation (current & saving)

        ⚠️ All endpoints require JWT authentication (Bearer token).
        """
)
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/v1/accounts")
public interface BankAccountController {

    @Operation(summary = "Get all accounts")
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AccountDTO.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @GetMapping("/all")
    ResponseEntity<List<AccountDTO>> getAllAccounts();

    @Operation(summary = "Get account by ID")
    @ApiResponse(responseCode = "200", description = "Account found",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{accountId}")
    ResponseEntity<AccountDTO> getAccountById(
            @Parameter(description = "Unique identifier of the account", example = "7ba95f32-1234-4321-abcd-2c963f66afa6")
            @PathVariable UUID accountId);

    @Operation(summary = "Get paginated accounts")
    @ApiResponse(responseCode = "200", description = "Paginated accounts retrieved successfully",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AccountDTO.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @GetMapping
    ResponseEntity<List<AccountDTO>> getAccountsPaginated(
            @Parameter(description = "Page number (0-based)", example = "0")
            @Min(0) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of accounts per page", example = "5")
            @Min(1) @RequestParam(defaultValue = "5") int size);

    @Operation(summary = "Get account operations")
    @ApiResponse(responseCode = "200", description = "Operations retrieved successfully",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = OperationDTO.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{accountId}/operations")
    ResponseEntity<List<OperationDTO>> getAccountOperations(
            @Parameter(description = "Unique identifier of the account", example = "7ba95f32-1234-4321-abcd-2c963f66afa6")
            @PathVariable UUID accountId);

    @Operation(summary = "Get account history (paginated operations)")
    @ApiResponse(responseCode = "200", description = "Account history retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountHistoryDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{accountId}/history")
    ResponseEntity<AccountHistoryDTO> getAccountHistory(
            @Parameter(description = "Unique identifier of the account", example = "7ba95f32-1234-4321-abcd-2c963f66afa6")
            @PathVariable UUID accountId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of operations per page", example = "6")
            @RequestParam(defaultValue = "6") int size);


    // =========================
    // FINANCIAL OPERATIONS
    // =========================

    @Operation(
            summary = "Debit account",
            description = "Debits an amount from an account (checks balance before operation)."
    )
    @ApiResponse(responseCode = "200", description = "Debit successful")
    @ApiResponse(responseCode = "400", description = "Invalid request (validation error)")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "422", description = "Insufficient balance")
    @PostMapping("/{accountId}/debit")
    ResponseEntity<Void> performDebit(
            @Parameter(description = "Unique identifier of the account to debit", example = "7ba95f32-1234-4321-abcd-2c963f66afa6")
            @PathVariable UUID accountId,
            @Valid @RequestBody DebitRequestDTO debitDTO);

    @Operation(
            summary = "Credit account",
            description = "Credits an amount to an account."
    )
    @ApiResponse(responseCode = "200", description = "Credit successful")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @PostMapping("/{accountId}/credit")
    ResponseEntity<Void> performCredit(
            @Parameter(description = "Unique identifier of the account to credit", example = "7ba95f32-1234-4321-abcd-2c963f66afa6")
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditRequestDTO creditDTO);

    @Operation(
            summary = "Transfer money between accounts",
            description = """
            Transfers money from a source account to a destination account.

            Steps:
            - Debit source account
            - Credit destination account
            - Record both operations in history
            """
    )
    @ApiResponse(responseCode = "200", description = "Transfer successful")
    @ApiResponse(responseCode = "400", description = "Invalid transfer request (validation error)")
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Source or destination account not found")
    @ApiResponse(responseCode = "422", description = "Insufficient balance or same source/destination account")
    @PostMapping("/transfer")
    ResponseEntity<Void> performTransfer(@Valid @RequestBody TransferDTO transferDTO);

    // =========================
    // ACCOUNT CREATION
    // =========================

    @Operation(summary = "Create current account")
    @ApiResponse(responseCode = "201", description = "Current account created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PostMapping("/{customerId}/current-account")
    ResponseEntity<AccountDTO> createCurrentAccount(
            @Parameter(description = "Unique identifier of the customer", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID customerId,
            @Valid @RequestBody AccountDTO accountDTO);

    @Operation(summary = "Create saving account")
    @ApiResponse(responseCode = "201", description = "Saving account created",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountDTO.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized — valid JWT required")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PostMapping("/{customerId}/saving-account")
    ResponseEntity<AccountDTO> createSavingAccount(
            @Parameter(description = "Unique identifier of the customer", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID customerId,
            @Valid @RequestBody AccountDTO accountDTO);
}
