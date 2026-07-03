package com.barry.bank.application.services.impl;

import com.barry.bank.application.services.OperationService;
import com.barry.bank.domain.model.BankAccount;
import com.barry.bank.domain.model.Operation;
import com.barry.bank.domain.enumerations.OperationType;
import com.barry.bank.domain.exception.ResourceNotFoundException;
import com.barry.bank.persistence.repositories.BankAccountRepository;
import com.barry.bank.persistence.repositories.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.barry.bank.domain.enumerations.OperationType.CREDIT;
import static com.barry.bank.domain.enumerations.OperationType.DEBIT;

@Service
@Log4j2
@RequiredArgsConstructor
public class OperationServiceImpl implements OperationService {

    private final BankAccountRepository accountRepository;
    private final OperationRepository operationRepository;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    @Transactional
    public void debitAccount(UUID accountId, BigDecimal amount, String description) {
        if (accountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("AccountId must not be null and amount must be greater than zero");
        }

        BankAccount account = findAccountById(accountId);

        account.debit(amount);
        accountRepository.save(account);
        recordOperation(amount, account, DEBIT, description);

        log.info("Debit completed: accountId={}, amount={}", accountId, amount);
    }

    @Override
    @Transactional
    public void creditAccount(UUID accountId, BigDecimal amount, String description) {
        if (accountId == null || amount == null) {
            throw new IllegalArgumentException("AccountId or amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        BankAccount account = findAccountById(accountId);
        account.credit(amount);
        accountRepository.save(account);
        recordOperation(amount, account, CREDIT, description);

        log.info("Credit completed: accountId={}, amount={}", accountId, amount);
    }

    private BankAccount findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with Id: " + accountId));
    }

    private void recordOperation(BigDecimal amount, BankAccount account, OperationType type, String description) {
        String defaultDescription = type == DEBIT ? "Debit operation" : "Credit operation";
        Operation operation = Operation.builder()
                .account(account)
                .operationDate(LocalDateTime.now())
                .operationAmount(amount)
                .operationNumber(generateOperationNumber())
                .operationType(type)
                .description(StringUtils.isNotBlank(description) ? description : defaultDescription)
                .build();
        operationRepository.save(operation);
    }

    private String generateOperationNumber() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", RANDOM.nextInt(1_000_000));
        return "OP-" + datePart + "-" + randomPart;
    }
}
