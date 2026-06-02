package com.barry.bank.application.services.impl;

import com.barry.bank.application.services.OperationService;
import com.barry.bank.domain.entities.BankAccount;
import com.barry.bank.domain.entities.Operation;
import com.barry.bank.domain.entities.enums.OperationType;
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

import static com.barry.bank.domain.entities.enums.OperationType.CREDIT;
import static com.barry.bank.domain.entities.enums.OperationType.DEBIT;

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
            log.warn("Invalid debit attempt: accountId={}, amount={}", accountId, amount);
            throw new IllegalArgumentException("AccountId must not be null and amount must be greater than zero");
        }

        BankAccount account = findAccountById(accountId);

        if (account.getBalance().compareTo(amount) < 0) {
            log.error("Insufficient balance for debit: balance={}, amount={}", account.getBalance(), amount);
            throw new IllegalStateException("Insufficient balance for a debit transaction");
        }

        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.subtract(amount);

        updateAccountBalance(account, newBalance);
        recordOperation(amount, account, DEBIT, description);

        log.info("Debit completed: accountId={}, amount={}, oldBalance={}, newBalance={}, rib={}",
                accountId, amount, oldBalance, newBalance, account.getRib());
    }

    @Override
    @Transactional
    public void creditAccount(UUID accountId, BigDecimal amount, String description) {
        if (accountId == null || amount == null) {
            log.warn("Null accountId or amount for credit: accountId={}, amount={}", accountId, amount);
            throw new IllegalArgumentException("AccountId or amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Credit amount must be greater than zero: accountId={}, amount={}", accountId, amount);
            throw new IllegalArgumentException("Credit amount must be greater than zero");
        }

        BankAccount account = findAccountById(accountId);
        BigDecimal oldBalance = account.getBalance();
        BigDecimal newBalance = oldBalance.add(amount);

        updateAccountBalance(account, newBalance);
        recordOperation(amount, account, CREDIT, description);

        log.info("Credit completed: accountId={}, amount={}, oldBalance={}, newBalance={}, rib={}",
                accountId, amount, oldBalance, newBalance, account.getRib());
    }

    private BankAccount findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> {
                    log.warn("Account not found: {}", accountId);
                    return new IllegalArgumentException("Account not found with Id: " + accountId);
                });
    }

    private void updateAccountBalance(BankAccount account, BigDecimal newBalance) {
        account.setBalance(newBalance);
        accountRepository.save(account);
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