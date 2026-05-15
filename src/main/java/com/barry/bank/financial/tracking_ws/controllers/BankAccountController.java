package com.barry.bank.financial.tracking_ws.controllers;

import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.AccountHistoryDTO;
import com.barry.bank.financial.tracking_ws.dtos.CreditRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.CurrentAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.DebitRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.OperationDTO;
import com.barry.bank.financial.tracking_ws.dtos.SavingAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.TransferDTO;
import io.swagger.v3.oas.annotations.Operation;
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
    @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
    @GetMapping("/all")
    ResponseEntity<List<AccountDTO>> getAllAccounts();

    @Operation(summary = "Get account by ID")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @GetMapping("/{accountId}")
    ResponseEntity<AccountDTO> getAccountById(@PathVariable UUID accountId);

    @Operation(summary = "Get paginated accounts")
    @ApiResponse(responseCode = "200", description = "Paginated accounts retrieved successfully")
    @GetMapping
    ResponseEntity<List<AccountDTO>> getAccountsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size);

    @Operation(summary = "Get account operations")
    @ApiResponse(responseCode = "200", description = "Operations retrieved successfully")
    @GetMapping("/{accountId}/operations")
    ResponseEntity<List<OperationDTO>> getAccountOperations(@PathVariable UUID accountId);


    @Operation(summary = "Get account history (paginated operations)")
    @ApiResponse(responseCode = "200", description = "Account history retrieved successfully")
    @GetMapping("/{accountId}/history")
    ResponseEntity<AccountHistoryDTO> getAccountHistory(@PathVariable UUID accountId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "6") int size);


    // =========================
    // FINANCIAL OPERATIONS
    // =========================

    @Operation(
            summary = "Debit account",
            description = "Debits an amount from an account (checks balance before operation)."
    )
    @ApiResponse(responseCode = "200", description = "Debit successful")
    @ApiResponse(responseCode = "400", description = "Invalid request or insufficient balance")
    @PostMapping("/{accountId}/debit")
    ResponseEntity<Void> performDebit(@PathVariable UUID accountId,
                                      @Valid @RequestBody DebitRequestDTO debitDTO);


    @Operation(
            summary = "Credit account",
            description = "Credits an amount to an account."
    )
    @ApiResponse(responseCode = "200", description = "Credit successful")
    @PostMapping("/{accountId}/credit")
    ResponseEntity<Void> performCredit(@PathVariable UUID accountId,
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
    @ApiResponse(responseCode = "400", description = "Invalid transfer request")
    @PostMapping("/transfer")
    ResponseEntity<Void> performTransfer(@Valid @RequestBody TransferDTO transferDTO);

    // =========================
    // ACCOUNT CREATION
    // =========================

    @Operation(summary = "Create current account")
    @ApiResponse(responseCode = "201", description = "Current account created")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PostMapping("/{customerId}/current-account")
    ResponseEntity<CurrentAccountDTO> createCurrentAccount(@PathVariable UUID customerId,
                                                     @RequestBody CurrentAccountDTO accountDTO);

    @Operation(summary = "Create saving account")
    @ApiResponse(responseCode = "201", description = "Saving account created")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PostMapping("/{customerId}/saving-account")
    ResponseEntity<SavingAccountDTO> createSavingAccount(@PathVariable UUID customerId,
                                                         @RequestBody SavingAccountDTO accountDTO);
}
