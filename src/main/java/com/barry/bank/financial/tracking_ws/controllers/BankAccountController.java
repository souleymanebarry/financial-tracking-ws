package com.barry.bank.financial.tracking_ws.controllers;

import com.barry.bank.financial.tracking_ws.dtos.AccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.AccountHistoryDTO;
import com.barry.bank.financial.tracking_ws.dtos.CreditRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.CurrentAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.DebitRequestDTO;
import com.barry.bank.financial.tracking_ws.dtos.OperationDTO;
import com.barry.bank.financial.tracking_ws.dtos.SavingAccountDTO;
import com.barry.bank.financial.tracking_ws.dtos.TransferDTO;
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

@RequestMapping("api/v1/accounts")
public interface BankAccountController {

    @GetMapping("/all")
    ResponseEntity<List<AccountDTO>> getAllAccounts();

    @GetMapping("/{accountId}")
    ResponseEntity<AccountDTO> getAccountById(@PathVariable UUID accountId);

    @GetMapping
    ResponseEntity<List<AccountDTO>> getAccountsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size);

    @GetMapping("/{accountId}/operations")
    ResponseEntity<List<OperationDTO>> getAccountOperations(@PathVariable UUID accountId);

    @GetMapping("/{accountId}/history")
    ResponseEntity<AccountHistoryDTO> getAccountHistory(@PathVariable UUID accountId,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "6") int size);

    @PostMapping("/{accountId}/debit")
    ResponseEntity<Void> performDebit(@PathVariable UUID accountId,
                                      @Valid @RequestBody DebitRequestDTO debitDTO);

    @PostMapping("/{accountId}/credit")
    ResponseEntity<Void> performCredit(@PathVariable UUID accountId,
                                      @Valid @RequestBody CreditRequestDTO creditDTO);

    @PostMapping("/transfer")
    ResponseEntity<Void> performTransfer(@Valid @RequestBody TransferDTO transferDTO);

    @PostMapping("/{customerId}/current-account")
    ResponseEntity<CurrentAccountDTO> createCurrentAccount(@PathVariable UUID customerId,
                                                     @RequestBody CurrentAccountDTO accountDTO);

    @PostMapping("/{customerId}/saving-account")
    ResponseEntity<SavingAccountDTO> createSavingAccount(@PathVariable UUID customerId,
                                                         @RequestBody SavingAccountDTO accountDTO);
}
